package AssessX_backend.service;

import AssessX_backend.dto.HintResponseDto;
import AssessX_backend.exception.HintAlreadyUsedException;
import AssessX_backend.exception.OllamaUnavailableException;
import AssessX_backend.model.Assignment;
import AssessX_backend.model.CodePractice;
import AssessX_backend.model.PracticeUnitTest;
import AssessX_backend.model.User;
import AssessX_backend.repository.AssignmentRepository;
import AssessX_backend.repository.CodePracticeRepository;
import AssessX_backend.repository.PracticeHintRepository;
import AssessX_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HintServiceTest {

    @Mock private PracticeHintRepository practiceHintRepository;
    @Mock private CodePracticeRepository practiceRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OllamaClient ollamaClient;

    private HintService hintService;

    private CodePractice practice;
    private Assignment assignment;
    private User student;

    @BeforeEach
    void setUp() {
        hintService = new HintService(
            practiceHintRepository, practiceRepository,
            assignmentRepository, userRepository, ollamaClient
        );

        student = new User();
        student.setId(10L);
        student.setGithubLogin("bob");

        PracticeUnitTest unitTest = new PracticeUnitTest();
        unitTest.setTestCode("assert new Solution().sum(2, 3) == 5;");

        practice = new CodePractice();
        practice.setId(1L);
        practice.setTitle("Sum");
        practice.setDescription("Write sum(a,b)");
        practice.setPoints(100);
        practice.setTimeLimitSec(30);
        practice.getUnitTests().add(unitTest);

        assignment = new Assignment();
        assignment.setId(5L);
        assignment.setPractice(practice);
    }

    @Test
    void requestHint_happyPath_returnsHint() {
        when(practiceHintRepository.existsByUserIdAndAssignmentId(10L, 5L)).thenReturn(false);
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(ollamaClient.generate(anyString())).thenReturn("Think about using the + operator.");

        HintResponseDto result = hintService.requestHint(1L, 5L, "public class Solution {}", 10L);

        assertThat(result.getHint()).isEqualTo("Think about using the + operator.");
        assertThat(result.getHintUsedAt()).isNotNull();
        verify(practiceHintRepository).save(argThat(h ->
            h.getUser().equals(student) &&
            h.getPractice().equals(practice) &&
            h.getAssignment().equals(assignment) &&
            h.getHintText().equals("Think about using the + operator.")
        ));
    }

    @Test
    void requestHint_nullCurrentCode_usesPlaceholder() {
        when(practiceHintRepository.existsByUserIdAndAssignmentId(10L, 5L)).thenReturn(false);
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(ollamaClient.generate(anyString())).thenReturn("Start by defining the method signature.");

        HintResponseDto result = hintService.requestHint(1L, 5L, null, 10L);

        assertThat(result.getHint()).isEqualTo("Start by defining the method signature.");
        verify(ollamaClient).generate(argThat(prompt -> prompt.contains("(no code yet)")));
    }

    @Test
    void requestHint_hintAlreadyUsed_throwsHintAlreadyUsedException() {
        when(practiceHintRepository.existsByUserIdAndAssignmentId(10L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> hintService.requestHint(1L, 5L, "", 10L))
                .isInstanceOf(HintAlreadyUsedException.class)
                .hasMessageContaining("5");

        verify(practiceRepository, never()).findById(any());
        verify(ollamaClient, never()).generate(any());
    }

    @Test
    void requestHint_ollamaUnavailable_propagatesException() {
        when(practiceHintRepository.existsByUserIdAndAssignmentId(10L, 5L)).thenReturn(false);
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(ollamaClient.generate(anyString())).thenThrow(new OllamaUnavailableException());

        assertThatThrownBy(() -> hintService.requestHint(1L, 5L, "", 10L))
                .isInstanceOf(OllamaUnavailableException.class)
                .hasMessageContaining("AI hint service unavailable");

        verify(practiceHintRepository, never()).save(any());
    }
}

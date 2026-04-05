package AssessX_backend.service;

import AssessX_backend.dto.ResultResponseDto;
import AssessX_backend.exception.ResultNotFoundException;
import AssessX_backend.model.*;
import AssessX_backend.repository.ResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;

    private ResultService resultService;

    private User student;
    private Group group;
    private AssessX_backend.model.Test test;
    private Assignment assignment;
    private Result result;

    @BeforeEach
    void setUp() {
        resultService = new ResultService(resultRepository);

        student = new User();
        student.setId(2L);
        student.setGithubLogin("student");
        student.setRole(User.Role.STUDENT);

        group = new Group();
        group.setId(10L);
        group.setName("CS-1");

        test = new AssessX_backend.model.Test();
        test.setId(100L);
        test.setTitle("Java Basics");

        assignment = new Assignment();
        assignment.setId(1L);
        assignment.setGroup(group);
        assignment.setTest(test);

        result = new Result();
        result.setId(42L);
        result.setUser(student);
        result.setAssignment(assignment);
        result.setTest(test);
        result.setAttemptNumber(1);
        result.setPoints(20);
        result.setMaxPoints(30);
        result.setStartedAt(LocalDateTime.now().minusMinutes(5));
        result.setSubmittedAt(LocalDateTime.now());
    }

    @Test
    void getMyResults_returnsResultsForUser() {
        when(resultRepository.findByUserId(2L)).thenReturn(List.of(result));

        List<ResultResponseDto> results = resultService.getMyResults(2L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(42L);
        assertThat(results.get(0).getUserId()).isEqualTo(2L);
        assertThat(results.get(0).getAssignmentId()).isEqualTo(1L);
        assertThat(results.get(0).getTestId()).isEqualTo(100L);
        assertThat(results.get(0).getPracticeId()).isNull();
        assertThat(results.get(0).getPoints()).isEqualTo(20);
        assertThat(results.get(0).getMaxPoints()).isEqualTo(30);
        verify(resultRepository).findByUserId(2L);
    }

    @Test
    void getMyResults_noResults_returnsEmptyList() {
        when(resultRepository.findByUserId(2L)).thenReturn(List.of());

        List<ResultResponseDto> results = resultService.getMyResults(2L);

        assertThat(results).isEmpty();
    }

    @Test
    void getGroupResults_returnsResultsForGroup() {
        when(resultRepository.findByGroupId(10L)).thenReturn(List.of(result));

        List<ResultResponseDto> results = resultService.getGroupResults(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(42L);
        assertThat(results.get(0).getUserId()).isEqualTo(2L);
        verify(resultRepository).findByGroupId(10L);
    }

    @Test
    void getGroupResults_noResults_returnsEmptyList() {
        when(resultRepository.findByGroupId(10L)).thenReturn(List.of());

        List<ResultResponseDto> results = resultService.getGroupResults(10L);

        assertThat(results).isEmpty();
    }

    @Test
    void getResultById_existingId_returnsDto() {
        when(resultRepository.findById(42L)).thenReturn(Optional.of(result));

        ResultResponseDto dto = resultService.getResultById(42L);

        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getUserId()).isEqualTo(2L);
        assertThat(dto.getAssignmentId()).isEqualTo(1L);
        assertThat(dto.getAttemptNumber()).isEqualTo(1);
        assertThat(dto.getPoints()).isEqualTo(20);
        assertThat(dto.getMaxPoints()).isEqualTo(30);
    }

    @Test
    void getResultById_notFound_throwsResultNotFoundException() {
        when(resultRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.getResultById(99L))
                .isInstanceOf(ResultNotFoundException.class)
                .hasMessageContaining("Result not found");
    }

    @Test
    void getResultById_practiceResult_returnsPracticeId() {
        CodePractice practice = new CodePractice();
        practice.setId(200L);
        practice.setTitle("FizzBuzz");

        Result practiceResult = new Result();
        practiceResult.setId(43L);
        practiceResult.setUser(student);
        practiceResult.setAssignment(assignment);
        practiceResult.setPractice(practice);
        practiceResult.setAttemptNumber(1);
        practiceResult.setPoints(15);
        practiceResult.setMaxPoints(20);
        practiceResult.setStartedAt(LocalDateTime.now());

        when(resultRepository.findById(43L)).thenReturn(Optional.of(practiceResult));

        ResultResponseDto dto = resultService.getResultById(43L);

        assertThat(dto.getPracticeId()).isEqualTo(200L);
        assertThat(dto.getTestId()).isNull();
    }
}

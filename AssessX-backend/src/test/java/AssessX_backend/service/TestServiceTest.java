package AssessX_backend.service;

import AssessX_backend.dto.CreateTestRequest;
import AssessX_backend.dto.SubmitTestRequest;
import AssessX_backend.dto.TestResponseDto;
import AssessX_backend.dto.TestSubmitResultDto;
import AssessX_backend.model.Test;
import AssessX_backend.model.User;
import AssessX_backend.exception.TestNotFoundException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.repository.TestRepository;
import AssessX_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private UserRepository userRepository;

    private TestService testService;

    private User teacher;
    private Test test;

    @BeforeEach
    void setUp() {
        testService = new TestService(testRepository, userRepository, new ObjectMapper());

        teacher = new User();
        teacher.setId(1L);
        teacher.setGithubId(100L);
        teacher.setGithubLogin("alice");
        teacher.setName("Alice");
        teacher.setRole(User.Role.TEACHER);

        test = new Test();
        test.setId(1L);
        test.setTitle("Java Basics");
        test.setQuestions("[{\"id\":\"1\",\"text\":\"What is Java?\"}]");
        test.setAnswers("{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\"}");
        test.setPoints(30);
        test.setTimeLimitSec(3600);
        test.setCreatedBy(teacher);
    }

    @org.junit.jupiter.api.Test
    void getAllTests_returnsAll() {
        when(testRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(test));

        List<TestResponseDto> result = testService.getAllTests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Java Basics");
        assertThat(result.get(0).getAnswers()).isNull();
    }

    @org.junit.jupiter.api.Test
    void getTestById_asTeacher_includesAnswers() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        TestResponseDto result = testService.getTestById(1L, "TEACHER");

        assertThat(result.getAnswers()).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void getTestById_asStudent_excludesAnswers() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        TestResponseDto result = testService.getTestById(1L, "STUDENT");

        assertThat(result.getAnswers()).isNull();
    }

    @org.junit.jupiter.api.Test
    void getTestById_notFound_throws404() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.getTestById(99L, "STUDENT"))
                .isInstanceOf(TestNotFoundException.class)
                .hasMessageContaining("Test not found");
    }

    @org.junit.jupiter.api.Test
    void createTest_success() {
        CreateTestRequest req = new CreateTestRequest();
        req.setTitle("Java Basics");
        req.setQuestions("[{\"id\":\"1\",\"text\":\"What is Java?\"}]");
        req.setAnswers("{\"1\":\"A\"}");
        req.setPoints(10);
        req.setTimeLimitSec(1800);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(testRepository.save(any(Test.class))).thenReturn(test);

        TestResponseDto result = testService.createTest(req, 1L);

        assertThat(result.getTitle()).isEqualTo("Java Basics");
        verify(testRepository).save(any(Test.class));
    }

    @org.junit.jupiter.api.Test
    void createTest_userNotFound_throws404() {
        CreateTestRequest req = new CreateTestRequest();
        req.setTitle("Test");
        req.setQuestions("[]");
        req.setAnswers("{}");
        req.setPoints(10);
        req.setTimeLimitSec(60);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.createTest(req, 99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @org.junit.jupiter.api.Test
    void updateTest_success() {
        CreateTestRequest req = new CreateTestRequest();
        req.setTitle("Updated Title");
        req.setQuestions("[{\"id\":\"1\",\"text\":\"Updated?\"}]");
        req.setAnswers("{\"1\":\"B\"}");
        req.setPoints(20);
        req.setTimeLimitSec(900);

        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(testRepository.save(any(Test.class))).thenAnswer(inv -> inv.getArgument(0));

        TestResponseDto result = testService.updateTest(1L, req);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getPoints()).isEqualTo(20);
    }

    @org.junit.jupiter.api.Test
    void deleteTest_success() {
        when(testRepository.existsById(1L)).thenReturn(true);

        testService.deleteTest(1L);

        verify(testRepository).deleteById(1L);
    }

    @org.junit.jupiter.api.Test
    void deleteTest_notFound_throws404() {
        when(testRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> testService.deleteTest(99L))
                .isInstanceOf(TestNotFoundException.class)
                .hasMessageContaining("Test not found");
    }

    @org.junit.jupiter.api.Test
    void submitTest_allCorrect_fullPoints() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        SubmitTestRequest req = new SubmitTestRequest();
        req.setAnswers(Map.of("1", "A", "2", "B", "3", "C"));

        TestSubmitResultDto result = testService.submitTest(1L, req);

        assertThat(result.getCorrectAnswers()).isEqualTo(3);
        assertThat(result.getTotalQuestions()).isEqualTo(3);
        assertThat(result.getEarnedPoints()).isEqualTo(30);
        assertThat(result.getMaxPoints()).isEqualTo(30);
    }

    @org.junit.jupiter.api.Test
    void submitTest_noneCorrect_zeroPoints() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        SubmitTestRequest req = new SubmitTestRequest();
        req.setAnswers(Map.of("1", "D", "2", "D", "3", "D"));

        TestSubmitResultDto result = testService.submitTest(1L, req);

        assertThat(result.getCorrectAnswers()).isEqualTo(0);
        assertThat(result.getEarnedPoints()).isEqualTo(0);
    }

    @org.junit.jupiter.api.Test
    void submitTest_partialCorrect_proportionalPoints() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));

        SubmitTestRequest req = new SubmitTestRequest();
        req.setAnswers(Map.of("1", "A", "2", "D", "3", "D"));

        TestSubmitResultDto result = testService.submitTest(1L, req);

        assertThat(result.getCorrectAnswers()).isEqualTo(1);
        assertThat(result.getTotalQuestions()).isEqualTo(3);
        assertThat(result.getEarnedPoints()).isEqualTo(10); // round(1/3 * 30) = 10
    }

    @org.junit.jupiter.api.Test
    void submitTest_testNotFound_throws404() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());

        SubmitTestRequest req = new SubmitTestRequest();
        req.setAnswers(Map.of("1", "A"));

        assertThatThrownBy(() -> testService.submitTest(99L, req))
                .isInstanceOf(TestNotFoundException.class)
                .hasMessageContaining("Test not found");
    }
}

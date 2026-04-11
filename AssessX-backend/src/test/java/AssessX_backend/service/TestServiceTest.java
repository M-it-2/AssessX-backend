package AssessX_backend.service;

import AssessX_backend.dto.CreateTestRequest;
import AssessX_backend.dto.SubmitTestRequest;
import AssessX_backend.dto.TestResponseDto;
import AssessX_backend.dto.TestSubmitResultDto;
import AssessX_backend.exception.*;
import AssessX_backend.model.Assignment;
import AssessX_backend.model.Group;
import AssessX_backend.model.User;
import AssessX_backend.repository.AssignmentRepository;
import AssessX_backend.repository.ResultRepository;
import AssessX_backend.repository.TestRepository;
import AssessX_backend.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock private TestRepository testRepository;
    @Mock private UserRepository userRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ResultRepository resultRepository;

    private TestService testService;
    private ObjectMapper objectMapper;

    private User teacher;
    private AssessX_backend.model.Test test;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testService = new TestService(
            testRepository,
            userRepository,
            assignmentRepository,
            resultRepository,
            objectMapper
        );

        teacher = createTeacher();
        test = createTest();
    }

    private User createTeacher() {
        User u = new User();
        u.setId(1L);
        u.setRole(User.Role.TEACHER);
        u.setName("Alice");
        return u;
    }

    private AssessX_backend.model.Test createTest() {
        AssessX_backend.model.Test t = new AssessX_backend.model.Test();
        t.setId(1L);
        t.setTitle("Java Basics");
        t.setAnswers("{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\"}");
        t.setQuestions("{\"list\":[]}");
        t.setPoints(30);
        t.setTimeLimitSec(3600);
        t.setCreatedBy(teacher);
        return t;
    }

    private SubmitTestRequest req(JsonNode answers) {
        SubmitTestRequest r = new SubmitTestRequest();
        r.setAnswers(answers);
        return r;
    }

    private JsonNode answers(String a1, String a2, String a3) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("1", a1);
        node.put("2", a2);
        node.put("3", a3);
        return node;
    }

    private String role(User.Role role) {
        return role.name();
    }

    @Test
    void getAllTests_returnsAll() {
        when(testRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(test));

        List<TestResponseDto> result = testService.getAllTests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Java Basics");
        assertThat(result.get(0).getAnswers()).isNull();
    }

    @Test
    void getTestById_teacher_seesAnswers() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        TestResponseDto result = testService.getTestById(1L, role(User.Role.TEACHER));
        assertThat(result.getAnswers()).isNotNull();
    }

    @Test
    void getTestById_student_hidesAnswers() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        TestResponseDto result = testService.getTestById(1L, role(User.Role.STUDENT));
        assertThat(result.getAnswers()).isNull();
    }

    @Test
    void getTestById_notFound() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            testService.getTestById(99L, role(User.Role.STUDENT))
        ).isInstanceOf(TestNotFoundException.class);
    }

    @Test
    void createTest_success() {
        CreateTestRequest req = new CreateTestRequest();
        req.setTitle("Java Basics");

        ObjectNode questions = objectMapper.createObjectNode();
        questions.putArray("list");
        req.setQuestions(questions);

        ObjectNode answers = objectMapper.createObjectNode();
        answers.put("1", "A");
        req.setAnswers(answers);
        req.setPoints(10);
        req.setTimeLimitSec(1800);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(testRepository.save(any(AssessX_backend.model.Test.class))).thenReturn(test);

        TestResponseDto result = testService.createTest(req, 1L);

        assertThat(result.getTitle()).isEqualTo("Java Basics");
        verify(testRepository).save(any(AssessX_backend.model.Test.class));
    }

    @Test
    void createTest_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            testService.createTest(new CreateTestRequest(), 99L)
        ).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateTest_success() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(testRepository.save(any(AssessX_backend.model.Test.class)))
            .thenAnswer(i -> i.getArgument(0));

        CreateTestRequest req = new CreateTestRequest();
        req.setTitle("Updated");

        TestResponseDto result = testService.updateTest(1L, req);
        assertThat(result.getTitle()).isEqualTo("Updated");
    }

    @Test
    void deleteTest_success() {
        when(testRepository.existsById(1L)).thenReturn(true);
        testService.deleteTest(1L);
        verify(testRepository).deleteById(1L);
    }

    @Test
    void deleteTest_notFound() {
        when(testRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() ->
            testService.deleteTest(99L)
        ).isInstanceOf(TestNotFoundException.class);
    }

    @Test
    void submit_allCorrect_fullScore() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        TestSubmitResultDto result = testService.submitTest(1L, req(answers("A", "B", "C")), 1L);
        assertThat(result.getCorrectAnswers()).isEqualTo(3);
        assertThat(result.getEarnedPoints()).isEqualTo(30);
    }

    @Test
    void submit_noneCorrect_zeroScore() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        TestSubmitResultDto result = testService.submitTest(1L, req(answers("D", "D", "D")), 1L);
        assertThat(result.getEarnedPoints()).isZero();
    }

    @Test
    void submit_partialCorrect() {
        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        TestSubmitResultDto result = testService.submitTest(1L, req(answers("A", "D", "D")), 1L);
        assertThat(result.getCorrectAnswers()).isEqualTo(1);
        assertThat(result.getEarnedPoints()).isEqualTo(10);
    }

    @Test
    void submit_testNotFound() {
        when(testRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            testService.submitTest(99L, req(answers("A", "A", "A")), 1L)
        ).isInstanceOf(TestNotFoundException.class);
    }

    @Test
    void submit_assignmentMismatch() {
        AssessX_backend.model.Test other = new AssessX_backend.model.Test();
        other.setId(99L);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setTest(other);

        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        SubmitTestRequest r = req(answers("A", "B", "C"));
        r.setAssignmentId(10L);

        assertThatThrownBy(() ->
            testService.submitTest(1L, r, 1L)
        ).isInstanceOf(InvalidAssignmentException.class);
    }

    @Test
    void submit_deadlineExpired() {
        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setTest(test);
        assignment.setDeadline(LocalDateTime.now().minusHours(1));

        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        SubmitTestRequest r = req(answers("A", "B", "C"));
        r.setAssignmentId(10L);

        assertThatThrownBy(() ->
            testService.submitTest(1L, r, 1L)
        ).isInstanceOf(DeadlineExpiredException.class);
    }

    @Test
    void submit_studentNotInGroup() {
        Group group = new Group();
        group.setId(5L);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setTest(test);
        assignment.setGroup(group);

        when(testRepository.findById(1L)).thenReturn(Optional.of(test));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        SubmitTestRequest r = req(answers("A", "B", "C"));
        r.setAssignmentId(10L);

        assertThatThrownBy(() ->
            testService.submitTest(1L, r, 42L)
        ).isInstanceOf(StudentNotInGroupException.class);
    }
}

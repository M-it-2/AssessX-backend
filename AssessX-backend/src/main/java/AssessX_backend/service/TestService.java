package AssessX_backend.service;

import AssessX_backend.dto.CreateTestRequest;
import AssessX_backend.dto.SubmitTestRequest;
import AssessX_backend.dto.TestResponseDto;
import AssessX_backend.dto.TestSubmitResultDto;
import AssessX_backend.exception.*;
import AssessX_backend.model.*;
import AssessX_backend.repository.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    public TestService(TestRepository testRepository,
                       UserRepository userRepository,
                       AssignmentRepository assignmentRepository,
                       ResultRepository resultRepository,
                       ObjectMapper objectMapper) {
        this.testRepository = testRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TestResponseDto> getAllTests() {
        return testRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(t -> TestResponseDto.from(t, false))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TestResponseDto getTestById(Long id, String role) {
        Test test = findTestById(id);
        boolean includeAnswers = "TEACHER".equals(role);
        return TestResponseDto.from(test, includeAnswers);
    }

    @Transactional
    public TestResponseDto createTest(CreateTestRequest request, Long userId) {
        User creator = findUserById(userId);

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setQuestions(toJson(request.getQuestions()));
        test.setAnswers(toJson(request.getAnswers()));
        test.setPoints(request.getPoints());
        test.setTimeLimitSec(request.getTimeLimitSec());
        test.setCreatedBy(creator);

        return TestResponseDto.from(testRepository.save(test), true);
    }

    @Transactional
    public TestResponseDto updateTest(Long id, CreateTestRequest request) {
        Test test = findTestById(id);

        test.setTitle(request.getTitle());
        test.setQuestions(toJson(request.getQuestions()));
        test.setAnswers(toJson(request.getAnswers()));
        test.setPoints(request.getPoints());
        test.setTimeLimitSec(request.getTimeLimitSec());

        return TestResponseDto.from(testRepository.save(test), true);
    }

    @Transactional
    public void deleteTest(Long id) {
        if (!testRepository.existsById(id)) {
            throw new TestNotFoundException(id);
        }
        List<Assignment> assignments = assignmentRepository.findByTestId(id);
        for (Assignment a : assignments) {
            resultRepository.deleteByAssignmentId(a.getId());
        }
        assignmentRepository.deleteAll(assignments);
        resultRepository.deleteByTestId(id);
        testRepository.deleteById(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TestSubmitResultDto submitTest(Long id, SubmitTestRequest request, Long userId) {
        Test test = findTestById(id);

        JsonNode correctAnswers = fromJson(test.getAnswers());
        JsonNode userAnswers = request.getAnswers();
        JsonNode questionsNode = fromJson(test.getQuestions());

        int total = correctAnswers.size();
        if (total == 0) {
            return new TestSubmitResultDto(0, test.getPoints(), 0, 0);
        }

        int correct = 0;
        for (int i = 0; i < total; i++) {
            int correctOptionIndex = correctAnswers.get(String.valueOf(i)).asInt();
            String userAnswer = userAnswers.has(String.valueOf(i))
                ? userAnswers.get(String.valueOf(i)).asText()
                : "";
            String correctOptionText = questionsNode.get(i)
                .get("options").get(correctOptionIndex).asText();
            if (correctOptionText.equals(userAnswer)) {
                correct++;
            }
        }

        int earned = (int) Math.round((double) correct / total * test.getPoints());

        if (request.getAssignmentId() != null && userId != null) {

            Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new AssignmentNotFoundException(request.getAssignmentId()));

            if (assignment.getTest() == null || !id.equals(assignment.getTest().getId())) {
                throw new InvalidAssignmentException("Assignment does not belong to this test");
            }

            if (assignment.getDeadline() != null &&
                LocalDateTime.now().isAfter(assignment.getDeadline())) {
                throw new DeadlineExpiredException();
            }

            Group group = assignment.getGroup();
            boolean isMember = group.getStudents()
                .stream()
                .anyMatch(s -> s.getId().equals(userId));

            if (!isMember) {
                throw new StudentNotInGroupException(userId, group.getId());
            }

            User user = findUserById(userId);

            int attemptNumber =
                resultRepository.countByUserIdAndAssignmentId(userId, assignment.getId()) + 1;

            Result result = new Result();
            result.setUser(user);
            result.setAssignment(assignment);
            result.setTest(test);
            result.setPoints(earned);
            result.setMaxPoints(test.getPoints());
            result.setAttemptNumber(attemptNumber);
            result.setSubmittedAt(LocalDateTime.now());

            resultRepository.save(result);
        }

        return new TestSubmitResultDto(earned, test.getPoints(), correct, total);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private JsonNode fromJson(String json) {
        if (json == null) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    private Test findTestById(Long id) {
        return testRepository.findById(id)
            .orElseThrow(() -> new TestNotFoundException(id));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
}

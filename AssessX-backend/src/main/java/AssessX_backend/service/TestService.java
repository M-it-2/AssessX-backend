package AssessX_backend.service;

import AssessX_backend.dto.CreateTestRequest;
import AssessX_backend.dto.SubmitTestRequest;
import AssessX_backend.dto.TestResponseDto;
import AssessX_backend.dto.TestSubmitResultDto;
import AssessX_backend.exception.TestAnswersParseException;
import AssessX_backend.exception.TestNotFoundException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.Test;
import AssessX_backend.model.User;
import AssessX_backend.repository.TestRepository;
import AssessX_backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TestService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TestService(TestRepository testRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.testRepository = testRepository;
        this.userRepository = userRepository;
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
        test.setQuestions(request.getQuestions());
        test.setAnswers(request.getAnswers());
        test.setPoints(request.getPoints());
        test.setTimeLimitSec(request.getTimeLimitSec());
        test.setCreatedBy(creator);
        return TestResponseDto.from(testRepository.save(test), true);
    }

    @Transactional
    public TestResponseDto updateTest(Long id, CreateTestRequest request) {
        Test test = findTestById(id);
        test.setTitle(request.getTitle());
        test.setQuestions(request.getQuestions());
        test.setAnswers(request.getAnswers());
        test.setPoints(request.getPoints());
        test.setTimeLimitSec(request.getTimeLimitSec());
        return TestResponseDto.from(testRepository.save(test), true);
    }

    @Transactional
    public void deleteTest(Long id) {
        if (!testRepository.existsById(id)) {
            throw new TestNotFoundException(id);
        }
        testRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TestSubmitResultDto submitTest(Long id, SubmitTestRequest request) {
        Test test = findTestById(id);
        Map<String, String> storedAnswers = parseAnswers(test.getAnswers(), id);
        int total = storedAnswers.size();
        if (total == 0) {
            return new TestSubmitResultDto(0, test.getPoints(), 0, 0);
        }
        int correct = 0;
        for (Map.Entry<String, String> entry : storedAnswers.entrySet()) {
            String submitted = request.getAnswers().get(entry.getKey());
            if (entry.getValue().equals(submitted)) {
                correct++;
            }
        }
        int earned = (int) Math.round((double) correct / total * test.getPoints());
        return new TestSubmitResultDto(earned, test.getPoints(), correct, total);
    }

    private Map<String, String> parseAnswers(String answersJson, Long testId) {
        try {
            return objectMapper.readValue(answersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new TestAnswersParseException(testId);
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

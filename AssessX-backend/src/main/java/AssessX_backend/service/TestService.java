package AssessX_backend.service;

import AssessX_backend.dto.CreateTestRequest;
import AssessX_backend.dto.SubmitTestRequest;
import AssessX_backend.dto.TestImportResultDto;
import AssessX_backend.dto.TestResponseDto;
import AssessX_backend.dto.TestSubmitResultDto;
import AssessX_backend.exception.*;
import AssessX_backend.model.*;
import AssessX_backend.repository.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TestService {

    private static final Logger log = LoggerFactory.getLogger(TestService.class);

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

        int total = correctAnswers.size();
        if (total == 0) {
            return new TestSubmitResultDto(0, test.getPoints(), 0, 0);
        }

        int correct = 0;
        for (String qId : correctAnswers.propertyNames()) {
            String correctAnswer = correctAnswers.get(qId).asText();
            String userAnswer = userAnswers != null && userAnswers.has(qId)
                ? userAnswers.get(qId).asText()
                : "";
            if (correctAnswer.equals(userAnswer)) {
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

            User user = findUserById(userId);

            if (user.getRole() == User.Role.STUDENT) {
                Group group = assignment.getGroup();
                boolean isMember = group.getStudents()
                    .stream()
                    .anyMatch(s -> s.getId().equals(userId));
                if (!isMember) {
                    throw new StudentNotInGroupException(userId, group.getId());
                }
            }

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

    @Transactional
    public TestImportResultDto importFromCsv(MultipartFile file, Long userId) {
        User creator = findUserById(userId);

        int createdTests = 0;
        int updatedTests = 0;
        List<String> failedRows = new ArrayList<>();

        record QuestionRow(String text, String optA, String optB, String optC, String optD, String correctOption) {}

        Map<String, List<QuestionRow>> questionsByTest = new LinkedHashMap<>();
        Map<String, Integer> pointsByTest = new LinkedHashMap<>();
        Map<String, Integer> timeLimitByTest = new LinkedHashMap<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {

            for (CSVRecord record : parser) {
                String rowLabel = "Row " + record.getRecordNumber();
                try {
                    String testTitle = record.get("test_title").trim();
                    String questionText = record.get("question_text").trim();
                    String optA = record.get("option_a").trim();
                    String optB = record.get("option_b").trim();
                    String optC = record.get("option_c").trim();
                    String optD = record.get("option_d").trim();
                    String correctOption = record.get("correct_option").trim().toLowerCase();
                    String pointsStr = record.get("points").trim();
                    String timeLimitStr = record.get("time_limit_sec").trim();

                    if (testTitle.isEmpty() || questionText.isEmpty() || correctOption.isEmpty()) {
                        failedRows.add(rowLabel + ": required fields are empty");
                        continue;
                    }

                    if (!Set.of("a", "b", "c", "d").contains(correctOption)) {
                        failedRows.add(rowLabel + ": invalid correct_option '" + correctOption + "', must be a/b/c/d");
                        continue;
                    }

                    int points;
                    try {
                        points = Integer.parseInt(pointsStr);
                    } catch (NumberFormatException e) {
                        failedRows.add(rowLabel + ": invalid points '" + pointsStr + "'");
                        continue;
                    }

                    int timeLimitSec;
                    try {
                        timeLimitSec = Integer.parseInt(timeLimitStr);
                    } catch (NumberFormatException e) {
                        failedRows.add(rowLabel + ": invalid time_limit_sec '" + timeLimitStr + "'");
                        continue;
                    }

                    if (points <= 0) {
                        failedRows.add(rowLabel + ": points must be > 0");
                        continue;
                    }

                    if (!questionsByTest.containsKey(testTitle)) {
                        questionsByTest.put(testTitle, new ArrayList<>());
                        pointsByTest.put(testTitle, points);
                        timeLimitByTest.put(testTitle, timeLimitSec);
                    }
                    questionsByTest.get(testTitle).add(
                        new QuestionRow(questionText, optA, optB, optC, optD, correctOption)
                    );

                } catch (Exception e) {
                    failedRows.add(rowLabel + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new CsvImportException("Failed to read CSV file: " + e.getMessage());
        }

        for (String testTitle : questionsByTest.keySet()) {
            List<QuestionRow> rows = questionsByTest.get(testTitle);

            var questionsNode = objectMapper.createArrayNode();
            var answersNode = objectMapper.createObjectNode();

            for (int i = 0; i < rows.size(); i++) {
                QuestionRow row = rows.get(i);
                String qId = String.valueOf(i + 1);

                ObjectNode question = objectMapper.createObjectNode();
                question.put("id", qId);
                question.put("text", row.text());
                ObjectNode options = objectMapper.createObjectNode();
                options.put("a", row.optA());
                options.put("b", row.optB());
                options.put("c", row.optC());
                options.put("d", row.optD());
                question.set("options", options);
                questionsNode.add(question);

                answersNode.put(qId, row.correctOption());
            }

            Optional<Test> existing = testRepository.findByTitle(testTitle);
            Test test;
            if (existing.isPresent()) {
                test = existing.get();
                test.setQuestions(toJson(questionsNode));
                test.setAnswers(toJson(answersNode));
                updatedTests++;
            } else {
                test = new Test();
                test.setTitle(testTitle);
                test.setQuestions(toJson(questionsNode));
                test.setAnswers(toJson(answersNode));
                test.setPoints(pointsByTest.get(testTitle));
                test.setTimeLimitSec(timeLimitByTest.get(testTitle));
                test.setCreatedBy(creator);
                createdTests++;
            }

            testRepository.save(test);
        }

        log.info("CSV test import by user {}: created {} tests, updated {} tests, {} failed rows",
                userId, createdTests, updatedTests, failedRows.size());

        return new TestImportResultDto(createdTests, updatedTests, failedRows);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof JsonNode node && node.isTextual()) {
                return node.asText();
            }
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

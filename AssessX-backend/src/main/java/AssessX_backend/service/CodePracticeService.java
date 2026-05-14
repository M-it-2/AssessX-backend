package AssessX_backend.service;

import AssessX_backend.dto.CodePracticeResponseDto;
import AssessX_backend.dto.CodeSubmissionResultDto;
import AssessX_backend.dto.CreateCodePracticeRequest;
import AssessX_backend.dto.CsvImportResultDto;
import AssessX_backend.dto.SubmitCodeRequest;
import AssessX_backend.exception.AssignmentNotFoundException;
import AssessX_backend.exception.CodePracticeNotFoundException;
import AssessX_backend.exception.CsvImportException;
import AssessX_backend.exception.DeadlineExpiredException;
import AssessX_backend.exception.InvalidAssignmentException;
import AssessX_backend.exception.StudentNotInGroupException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.Assignment;
import AssessX_backend.model.CodePractice;
import AssessX_backend.model.CodeSubmission;
import AssessX_backend.model.Group;
import AssessX_backend.model.PracticeUnitTest;
import AssessX_backend.model.Result;
import AssessX_backend.model.User;
import AssessX_backend.repository.AssignmentRepository;
import AssessX_backend.repository.CodePracticeRepository;
import AssessX_backend.repository.CodeSubmissionRepository;
import AssessX_backend.repository.PracticeHintRepository;
import AssessX_backend.repository.ResultRepository;
import AssessX_backend.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.stream.Collectors;

@Service
public class CodePracticeService {

    private static final Logger log = LoggerFactory.getLogger(CodePracticeService.class);

    private final CodePracticeRepository practiceRepository;
    private final UserRepository userRepository;
    private final CodeExecutionService codeExecutionService;
    private final AssignmentRepository assignmentRepository;
    private final ResultRepository resultRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;
    private final PracticeHintRepository practiceHintRepository;

    public CodePracticeService(CodePracticeRepository practiceRepository,
                               UserRepository userRepository,
                               CodeExecutionService codeExecutionService,
                               AssignmentRepository assignmentRepository,
                               ResultRepository resultRepository,
                               CodeSubmissionRepository codeSubmissionRepository,
                               PracticeHintRepository practiceHintRepository) {
        this.practiceRepository = practiceRepository;
        this.userRepository = userRepository;
        this.codeExecutionService = codeExecutionService;
        this.assignmentRepository = assignmentRepository;
        this.resultRepository = resultRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
        this.practiceHintRepository = practiceHintRepository;
    }

    @Transactional(readOnly = true)
    public List<CodePracticeResponseDto> getAllPractices() {
        return practiceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CodePracticeResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CodePracticeResponseDto getPracticeById(Long id) {
        return CodePracticeResponseDto.from(findPracticeById(id));
    }

    @Transactional
    public CodePracticeResponseDto createPractice(CreateCodePracticeRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        CodePractice practice = new CodePractice();
        practice.setTitle(request.getTitle());
        practice.setDescription(request.getDescription());
        practice.setPoints(request.getPoints());
        practice.setTimeLimitSec(request.getTimeLimitSec());
        practice.setCreatedBy(creator);

        if (request.getUnitTests() != null) {
            for (String testCode : request.getUnitTests()) {
                PracticeUnitTest unitTest = new PracticeUnitTest();
                unitTest.setTestCode(testCode);
                unitTest.setPractice(practice);
                practice.getUnitTests().add(unitTest);
            }
        }

        return CodePracticeResponseDto.from(practiceRepository.save(practice));
    }

    @Transactional
    public CodePracticeResponseDto updatePractice(Long id, CreateCodePracticeRequest request) {
        CodePractice practice = findPracticeById(id);
        practice.setTitle(request.getTitle());
        practice.setDescription(request.getDescription());
        practice.setPoints(request.getPoints());
        practice.setTimeLimitSec(request.getTimeLimitSec());

        practice.getUnitTests().clear();
        if (request.getUnitTests() != null) {
            for (String testCode : request.getUnitTests()) {
                PracticeUnitTest unitTest = new PracticeUnitTest();
                unitTest.setTestCode(testCode);
                unitTest.setPractice(practice);
                practice.getUnitTests().add(unitTest);
            }
        }

        return CodePracticeResponseDto.from(practiceRepository.save(practice));
    }

    @Transactional
    public void deletePractice(Long id) {
        if (!practiceRepository.existsById(id)) {
            throw new CodePracticeNotFoundException(id);
        }
        List<Assignment> assignments = assignmentRepository.findByPracticeId(id);
        for (Assignment a : assignments) {
            List<Result> results = resultRepository.findByAssignmentId(a.getId());
            for (Result r : results) {
                codeSubmissionRepository.deleteByResultId(r.getId());
            }
            resultRepository.deleteByAssignmentId(a.getId());
        }
        assignmentRepository.deleteAll(assignments);
        List<Result> practiceResults = resultRepository.findByPracticeId(id);
        for (Result r : practiceResults) {
            codeSubmissionRepository.deleteByResultId(r.getId());
        }
        resultRepository.deleteByPracticeId(id);
        practiceRepository.deleteById(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CodeSubmissionResultDto submitPractice(Long id, SubmitCodeRequest request, Long userId) {
        CodePractice practice = findPracticeById(id);
        List<String> unitTestCodes = practice.getUnitTests().stream()
                .map(PracticeUnitTest::getTestCode)
                .collect(Collectors.toList());
        CodeSubmissionResultDto executionResult = codeExecutionService.execute(
                request.getCode(), unitTestCodes, practice.getTimeLimitSec());

        if (request.getAssignmentId() != null && userId != null) {
            Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                    .orElseThrow(() -> new AssignmentNotFoundException(request.getAssignmentId()));
            if (assignment.getPractice() == null || !id.equals(assignment.getPractice().getId())) {
                throw new InvalidAssignmentException("Assignment does not belong to this practice");
            }
            if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
                throw new DeadlineExpiredException();
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            if (user.getRole() == User.Role.STUDENT) {
                Group group = assignment.getGroup();
                boolean isMember = group.getStudents().stream().anyMatch(s -> s.getId().equals(userId));
                if (!isMember) {
                    throw new StudentNotInGroupException(userId, group.getId());
                }
            }
            int attemptNumber = resultRepository.countByUserIdAndAssignmentId(userId, assignment.getId()) + 1;

            int total = executionResult.getTotalTests();
            int passed = executionResult.getPassedTests();
            boolean hintUsed = practiceHintRepository.existsByUserIdAndAssignmentId(userId, assignment.getId());
            int maxPoints = hintUsed ? practice.getPoints() / 2 : practice.getPoints();
            int earned = total > 0 ? (int) Math.round((double) passed / total * maxPoints) : 0;

            Result result = new Result();
            result.setUser(user);
            result.setAssignment(assignment);
            result.setPractice(practice);
            result.setPoints(earned);
            result.setMaxPoints(maxPoints);
            result.setAttemptNumber(attemptNumber);
            result.setSubmittedAt(LocalDateTime.now());
            Result savedResult = resultRepository.save(result);

            CodeSubmission submission = new CodeSubmission();
            submission.setResult(savedResult);
            submission.setCode(request.getCode());
            submission.setTestOutput(executionResult.getOutput());
            submission.setPassedTests(passed);
            submission.setTotalTests(total);
            codeSubmissionRepository.save(submission);
        }

        return executionResult;
    }

    @Transactional
    public CsvImportResultDto importFromCsv(MultipartFile file, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        int createdPractices = 0;
        int addedTests = 0;
        List<String> failedRows = new ArrayList<>();

        Map<String, List<String>> testCodesByTask = new LinkedHashMap<>();
        Map<String, String> taskDescriptions = new LinkedHashMap<>();
        Map<String, Integer> taskMaxScores = new LinkedHashMap<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {

            for (CSVRecord record : parser) {
                String rowLabel = "Row " + record.getRecordNumber();
                try {
                    String taskName = record.get("task_name").trim();
                    String taskDescription = record.get("task_description").trim();
                    String maxScoreStr = record.get("max_score").trim();
                    String testCode = record.get("test_code").trim();

                    if (taskName.isEmpty() || taskDescription.isEmpty() || testCode.isEmpty()) {
                        failedRows.add(rowLabel + ": required fields are empty");
                        continue;
                    }

                    int maxScore;
                    try {
                        maxScore = Integer.parseInt(maxScoreStr);
                    } catch (NumberFormatException e) {
                        failedRows.add(rowLabel + ": invalid max_score '" + maxScoreStr + "'");
                        continue;
                    }

                    if (maxScore <= 0) {
                        failedRows.add(rowLabel + ": max_score must be > 0");
                        continue;
                    }

                    if (!testCodesByTask.containsKey(taskName)) {
                        testCodesByTask.put(taskName, new ArrayList<>());
                        taskDescriptions.put(taskName, taskDescription);
                        taskMaxScores.put(taskName, maxScore);
                    }
                    testCodesByTask.get(taskName).add(testCode);

                } catch (Exception e) {
                    failedRows.add(rowLabel + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new CsvImportException("Failed to read CSV file: " + e.getMessage());
        }

        for (String taskName : testCodesByTask.keySet()) {
            List<String> testCodes = testCodesByTask.get(taskName);
            Optional<CodePractice> existing = practiceRepository.findByTitle(taskName);
            CodePractice practice;

            if (existing.isPresent()) {
                practice = existing.get();
            } else {
                practice = new CodePractice();
                practice.setTitle(taskName);
                practice.setDescription(taskDescriptions.get(taskName));
                practice.setPoints(taskMaxScores.get(taskName));
                practice.setTimeLimitSec(30);
                practice.setCreatedBy(creator);
                createdPractices++;
            }

            for (String testCode : testCodes) {
                PracticeUnitTest unitTest = new PracticeUnitTest();
                unitTest.setTestCode(testCode);
                unitTest.setPractice(practice);
                practice.getUnitTests().add(unitTest);
                addedTests++;
            }

            practiceRepository.save(practice);
        }

        log.info("CSV import by user {}: created {} practices, added {} tests, {} failed rows",
                userId, createdPractices, addedTests, failedRows.size());

        return new CsvImportResultDto(createdPractices, addedTests, failedRows);
    }

    private CodePractice findPracticeById(Long id) {
        return practiceRepository.findById(id)
                .orElseThrow(() -> new CodePracticeNotFoundException(id));
    }
}

package AssessX_backend.service;

import AssessX_backend.dto.CodePracticeResponseDto;
import AssessX_backend.dto.CodeSubmissionResultDto;
import AssessX_backend.dto.CreateCodePracticeRequest;
import AssessX_backend.dto.SubmitCodeRequest;
import AssessX_backend.exception.AssignmentNotFoundException;
import AssessX_backend.exception.CodePracticeNotFoundException;
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
import AssessX_backend.repository.ResultRepository;
import AssessX_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodePracticeService {

    private final CodePracticeRepository practiceRepository;
    private final UserRepository userRepository;
    private final CodeExecutionService codeExecutionService;
    private final AssignmentRepository assignmentRepository;
    private final ResultRepository resultRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;

    public CodePracticeService(CodePracticeRepository practiceRepository,
                               UserRepository userRepository,
                               CodeExecutionService codeExecutionService,
                               AssignmentRepository assignmentRepository,
                               ResultRepository resultRepository,
                               CodeSubmissionRepository codeSubmissionRepository) {
        this.practiceRepository = practiceRepository;
        this.userRepository = userRepository;
        this.codeExecutionService = codeExecutionService;
        this.assignmentRepository = assignmentRepository;
        this.resultRepository = resultRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
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
            Group group = assignment.getGroup();
            boolean isMember = group.getStudents().stream().anyMatch(s -> s.getId().equals(userId));
            if (!isMember) {
                throw new StudentNotInGroupException(userId, group.getId());
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            int attemptNumber = resultRepository.countByUserIdAndAssignmentId(userId, assignment.getId()) + 1;

            int total = executionResult.getTotalTests();
            int passed = executionResult.getPassedTests();
            int earned = total > 0 ? (int) Math.round((double) passed / total * practice.getPoints()) : 0;

            Result result = new Result();
            result.setUser(user);
            result.setAssignment(assignment);
            result.setPractice(practice);
            result.setPoints(earned);
            result.setMaxPoints(practice.getPoints());
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

    private CodePractice findPracticeById(Long id) {
        return practiceRepository.findById(id)
                .orElseThrow(() -> new CodePracticeNotFoundException(id));
    }
}

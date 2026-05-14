package AssessX_backend.service;

import AssessX_backend.dto.CodePracticeResponseDto;
import AssessX_backend.dto.CodeSubmissionResultDto;
import AssessX_backend.dto.CreateCodePracticeRequest;
import AssessX_backend.dto.CsvImportResultDto;
import AssessX_backend.dto.RunCodeResultDto;
import AssessX_backend.dto.RunCodeResultDto.TestRunResult;
import AssessX_backend.dto.SubmitCodeRequest;
import AssessX_backend.exception.CodePracticeNotFoundException;
import AssessX_backend.exception.DeadlineExpiredException;
import AssessX_backend.exception.InvalidAssignmentException;
import AssessX_backend.exception.StudentNotInGroupException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.Assignment;
import AssessX_backend.model.CodePractice;
import AssessX_backend.model.Group;
import AssessX_backend.model.PracticeUnitTest;
import AssessX_backend.model.User;
import AssessX_backend.repository.AssignmentRepository;
import AssessX_backend.repository.CodePracticeRepository;
import AssessX_backend.repository.CodeSubmissionRepository;
import AssessX_backend.repository.PracticeHintRepository;
import AssessX_backend.repository.ResultRepository;
import AssessX_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodePracticeServiceTest {

    @Mock private CodePracticeRepository practiceRepository;
    @Mock private UserRepository userRepository;
    @Mock private CodeExecutionService codeExecutionService;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private CodeSubmissionRepository codeSubmissionRepository;
    @Mock private PracticeHintRepository practiceHintRepository;

    private CodePracticeService practiceService;

    private User teacher;
    private CodePractice practice;

    @BeforeEach
    void setUp() {
        practiceService = new CodePracticeService(practiceRepository, userRepository, codeExecutionService,
                assignmentRepository, resultRepository, codeSubmissionRepository, practiceHintRepository);

        teacher = new User();
        teacher.setId(1L);
        teacher.setGithubLogin("alice");
        teacher.setRole(User.Role.TEACHER);

        practice = new CodePractice();
        practice.setId(1L);
        practice.setTitle("FizzBuzz");
        practice.setDescription("Implement FizzBuzz");
        practice.setPoints(20);
        practice.setTimeLimitSec(30);
        practice.setCreatedBy(teacher);

        PracticeUnitTest unitTest = new PracticeUnitTest();
        unitTest.setTestCode("assert new Solution().fizzBuzz(3).equals(\"Fizz\") : \"Expected Fizz\";");
        unitTest.setPractice(practice);
        practice.getUnitTests().add(unitTest);
    }

    @Test
    void getAllPractices_returnsList() {
        when(practiceRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(practice));

        List<CodePracticeResponseDto> result = practiceService.getAllPractices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("FizzBuzz");
        assertThat(result.get(0).getUnitTestCount()).isEqualTo(1);
    }

    @Test
    void getAllPractices_empty_returnsEmptyList() {
        when(practiceRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<CodePracticeResponseDto> result = practiceService.getAllPractices();

        assertThat(result).isEmpty();
    }

    @Test
    void getPracticeById_found_returnsDto() {
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));

        CodePracticeResponseDto result = practiceService.getPracticeById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("FizzBuzz");
        assertThat(result.getDescription()).isEqualTo("Implement FizzBuzz");
        assertThat(result.getPoints()).isEqualTo(20);
    }

    @Test
    void getPracticeById_notFound_throwsCodePracticeNotFoundException() {
        when(practiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practiceService.getPracticeById(99L))
                .isInstanceOf(CodePracticeNotFoundException.class)
                .hasMessageContaining("Code practice not found");
    }

    @Test
    void createPractice_success_savesWithUnitTests() {
        CreateCodePracticeRequest req = new CreateCodePracticeRequest();
        req.setTitle("FizzBuzz");
        req.setDescription("Implement FizzBuzz");
        req.setPoints(20);
        req.setTimeLimitSec(30);
        req.setUnitTests(List.of("assert true;", "assert 1 == 1;"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.save(any(CodePractice.class))).thenReturn(practice);

        CodePracticeResponseDto result = practiceService.createPractice(req, 1L);

        assertThat(result.getTitle()).isEqualTo("FizzBuzz");
        verify(practiceRepository).save(argThat(p ->
                p.getUnitTests().size() == 2 &&
                p.getCreatedBy().equals(teacher)
        ));
    }

    @Test
    void createPractice_noUnitTests_savesWithEmptyList() {
        CreateCodePracticeRequest req = new CreateCodePracticeRequest();
        req.setTitle("Simple");
        req.setDescription("desc");
        req.setPoints(10);
        req.setTimeLimitSec(60);
        req.setUnitTests(null);

        CodePractice emptyPractice = new CodePractice();
        emptyPractice.setId(2L);
        emptyPractice.setTitle("Simple");
        emptyPractice.setDescription("desc");
        emptyPractice.setPoints(10);
        emptyPractice.setTimeLimitSec(60);
        emptyPractice.setCreatedBy(teacher);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.save(any(CodePractice.class))).thenReturn(emptyPractice);

        CodePracticeResponseDto result = practiceService.createPractice(req, 1L);

        assertThat(result.getUnitTestCount()).isEqualTo(0);
    }

    @Test
    void createPractice_userNotFound_throwsUserNotFoundException() {
        CreateCodePracticeRequest req = new CreateCodePracticeRequest();
        req.setTitle("Test");
        req.setDescription("desc");
        req.setPoints(10);
        req.setTimeLimitSec(10);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practiceService.createPractice(req, 99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updatePractice_success_updatesAllFields() {
        CreateCodePracticeRequest req = new CreateCodePracticeRequest();
        req.setTitle("Updated Title");
        req.setDescription("New description");
        req.setPoints(30);
        req.setTimeLimitSec(60);
        req.setUnitTests(List.of("assert 2 + 2 == 4;"));

        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(practiceRepository.save(any(CodePractice.class))).thenAnswer(inv -> inv.getArgument(0));

        CodePracticeResponseDto result = practiceService.updatePractice(1L, req);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.getPoints()).isEqualTo(30);
        assertThat(result.getUnitTestCount()).isEqualTo(1);
    }

    @Test
    void updatePractice_notFound_throwsCodePracticeNotFoundException() {
        when(practiceRepository.findById(99L)).thenReturn(Optional.empty());

        CreateCodePracticeRequest req = new CreateCodePracticeRequest();
        req.setTitle("x");
        req.setDescription("x");
        req.setPoints(1);
        req.setTimeLimitSec(1);

        assertThatThrownBy(() -> practiceService.updatePractice(99L, req))
                .isInstanceOf(CodePracticeNotFoundException.class)
                .hasMessageContaining("Code practice not found");
    }

    @Test
    void deletePractice_success_callsDeleteById() {
        when(practiceRepository.existsById(1L)).thenReturn(true);

        practiceService.deletePractice(1L);

        verify(practiceRepository).deleteById(1L);
    }

    @Test
    void deletePractice_notFound_throwsCodePracticeNotFoundException() {
        when(practiceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> practiceService.deletePractice(99L))
                .isInstanceOf(CodePracticeNotFoundException.class)
                .hasMessageContaining("Code practice not found");
    }

    @Test
    void submitPractice_delegatesToCodeExecutionService() {
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        CodeSubmissionResultDto expected = new CodeSubmissionResultDto(1, 1, "RESULT:1/1\n");
        when(codeExecutionService.execute(anyString(), anyList(), anyInt())).thenReturn(expected);

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setCode("public class Solution { public String fizzBuzz(int n) { return \"Fizz\"; } }");

        CodeSubmissionResultDto result = practiceService.submitPractice(1L, req, null);

        assertThat(result.getPassedTests()).isEqualTo(1);
        assertThat(result.getTotalTests()).isEqualTo(1);
        verify(codeExecutionService).execute(eq(req.getCode()), anyList(), eq(30));
    }

    @Test
    void submitPractice_passesUnitTestCodesToExecutionService() {
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(codeExecutionService.execute(anyString(), anyList(), anyInt()))
                .thenReturn(new CodeSubmissionResultDto(0, 1, ""));

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setCode("public class Solution {}");

        practiceService.submitPractice(1L, req, null);

        verify(codeExecutionService).execute(
                anyString(),
                argThat(codes -> codes.size() == 1 && codes.get(0).contains("fizzBuzz")),
                eq(30)
        );
    }

    @Test
    void submitPractice_notFound_throwsCodePracticeNotFoundException() {
        when(practiceRepository.findById(99L)).thenReturn(Optional.empty());

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setCode("public class Solution {}");

        assertThatThrownBy(() -> practiceService.submitPractice(99L, req, null))
                .isInstanceOf(CodePracticeNotFoundException.class)
                .hasMessageContaining("Code practice not found");
    }

    @Test
    void submitPractice_assignmentMismatch_throwsInvalidAssignmentException() {
        CodePractice otherPractice = new CodePractice();
        otherPractice.setId(99L);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setPractice(otherPractice);

        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(codeExecutionService.execute(anyString(), anyList(), anyInt()))
                .thenReturn(new CodeSubmissionResultDto(1, 1, "RESULT:1/1\n"));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setAssignmentId(10L);
        req.setCode("public class Solution {}");

        assertThatThrownBy(() -> practiceService.submitPractice(1L, req, 1L))
                .isInstanceOf(InvalidAssignmentException.class)
                .hasMessageContaining("does not belong to this practice");
    }

    @Test
    void submitPractice_expiredDeadline_throwsDeadlineExpiredException() {
        CodePractice matchingPractice = new CodePractice();
        matchingPractice.setId(1L);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setPractice(matchingPractice);
        assignment.setDeadline(LocalDateTime.now().minusHours(1));

        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(codeExecutionService.execute(anyString(), anyList(), anyInt()))
                .thenReturn(new CodeSubmissionResultDto(1, 1, "RESULT:1/1\n"));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setAssignmentId(10L);
        req.setCode("public class Solution {}");

        assertThatThrownBy(() -> practiceService.submitPractice(1L, req, 1L))
                .isInstanceOf(DeadlineExpiredException.class)
                .hasMessageContaining("deadline has expired");
    }

    @Test
    void submitPractice_hintUsed_capsMaxPointsToHalf() {
        CodePractice matchingPractice = new CodePractice();
        matchingPractice.setId(1L);
        matchingPractice.setPoints(100);
        matchingPractice.setTimeLimitSec(30);

        PracticeUnitTest unitTest = new PracticeUnitTest();
        unitTest.setTestCode("assert true;");
        unitTest.setPractice(matchingPractice);
        matchingPractice.getUnitTests().add(unitTest);

        Group group = new Group();
        group.setId(5L);
        User student = new User();
        student.setId(2L);
        group.getStudents().add(student);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setPractice(matchingPractice);
        assignment.setGroup(group);

        when(practiceRepository.findById(1L)).thenReturn(Optional.of(matchingPractice));
        when(codeExecutionService.execute(anyString(), anyList(), anyInt()))
                .thenReturn(new CodeSubmissionResultDto(1, 1, "RESULT:1/1\n"));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(practiceHintRepository.existsByUserIdAndAssignmentId(2L, 10L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(resultRepository.countByUserIdAndAssignmentId(2L, 10L)).thenReturn(0);
        when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setAssignmentId(10L);
        req.setCode("public class Solution {}");

        practiceService.submitPractice(1L, req, 2L);

        verify(resultRepository).save(argThat(r -> r.getMaxPoints() == 50 && r.getPoints() == 50));
    }

    @Test
    void importFromCsv_newPractice_createsAndReturnsDto() throws Exception {
        String csvContent = "task_name,task_description,max_score,test_class_name,test_method_name,test_code\n" +
                "FizzBuzz,Implement FizzBuzz,20,SolutionTest,testFizz,assert true;\n";
        MockMultipartFile file = new MockMultipartFile("file", "practices.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.findByTitle("FizzBuzz")).thenReturn(Optional.empty());
        when(practiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResultDto result = practiceService.importFromCsv(file, 1L);

        assertThat(result.getCreatedPractices()).isEqualTo(1);
        assertThat(result.getAddedTests()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEmpty();
        verify(practiceRepository).save(argThat(p ->
                p.getTitle().equals("FizzBuzz") &&
                p.getPoints() == 20 &&
                p.getTimeLimitSec() == 30 &&
                p.getUnitTests().size() == 1
        ));
    }

    @Test
    void importFromCsv_existingPractice_addsTestsWithoutCreating() throws Exception {
        String csvContent = "task_name,task_description,max_score,test_class_name,test_method_name,test_code\n" +
                "FizzBuzz,Implement FizzBuzz,20,SolutionTest,testFizz,assert false;\n";
        MockMultipartFile file = new MockMultipartFile("file", "practices.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.findByTitle("FizzBuzz")).thenReturn(Optional.of(practice));
        when(practiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResultDto result = practiceService.importFromCsv(file, 1L);

        assertThat(result.getCreatedPractices()).isEqualTo(0);
        assertThat(result.getAddedTests()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEmpty();
    }

    @Test
    void importFromCsv_invalidRow_collectsFailedRowAndContinues() throws Exception {
        String csvContent = "task_name,task_description,max_score,test_class_name,test_method_name,test_code\n" +
                ",desc,20,SolutionTest,test,assert true;\n" +
                "Valid,desc,10,SolutionTest,test,assert true;\n";
        MockMultipartFile file = new MockMultipartFile("file", "practices.csv", "text/csv", csvContent.getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.findByTitle("Valid")).thenReturn(Optional.empty());
        when(practiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResultDto result = practiceService.importFromCsv(file, 1L);

        assertThat(result.getCreatedPractices()).isEqualTo(1);
        assertThat(result.getAddedTests()).isEqualTo(1);
        assertThat(result.getFailedRows()).hasSize(1);
        assertThat(result.getFailedRows().get(0)).contains("required fields are empty");
    }

    @Test
    void runCode_allTestsPass_returnsCompiledTrueAllPassed() {
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        RunCodeResultDto expected = new RunCodeResultDto(true,
                List.of(new TestRunResult("Test 1", true, null)), 1, 1);
        when(codeExecutionService.executeWithDetails(anyString(), anyList(), anyInt())).thenReturn(expected);

        RunCodeResultDto result = practiceService.runCode(1L, "public class Solution {}");

        assertThat(result.isCompiled()).isTrue();
        assertThat(result.getPassedCount()).isEqualTo(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).isPassed()).isTrue();
    }

    @Test
    void runCode_compilationError_returnsCompiledFalse() {
        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        RunCodeResultDto expected = new RunCodeResultDto(false, List.of(), 0, 1);
        when(codeExecutionService.executeWithDetails(anyString(), anyList(), anyInt())).thenReturn(expected);

        RunCodeResultDto result = practiceService.runCode(1L, "class Broken {{{");

        assertThat(result.isCompiled()).isFalse();
        assertThat(result.getPassedCount()).isEqualTo(0);
        assertThat(result.getResults()).isEmpty();
    }

    @Test
    void runCode_partialPass_returnsCorrectCounts() {
        PracticeUnitTest unitTest2 = new PracticeUnitTest();
        unitTest2.setTestCode("assert new Solution().fizzBuzz(5).equals(\"Buzz\");");
        unitTest2.setPractice(practice);
        practice.getUnitTests().add(unitTest2);

        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        RunCodeResultDto expected = new RunCodeResultDto(true,
                List.of(new TestRunResult("Test 1", true, null),
                        new TestRunResult("Test 2", false, "Expected Buzz")),
                1, 2);
        when(codeExecutionService.executeWithDetails(anyString(), anyList(), anyInt())).thenReturn(expected);

        RunCodeResultDto result = practiceService.runCode(1L, "public class Solution {}");

        assertThat(result.isCompiled()).isTrue();
        assertThat(result.getPassedCount()).isEqualTo(1);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getResults().get(1).isPassed()).isFalse();
        assertThat(result.getResults().get(1).getError()).isEqualTo("Expected Buzz");
    }

    @Test
    void runCode_notFound_throwsCodePracticeNotFoundException() {
        when(practiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practiceService.runCode(99L, "public class Solution {}"))
                .isInstanceOf(CodePracticeNotFoundException.class)
                .hasMessageContaining("Code practice not found");
    }

    @Test
    void submitPractice_studentNotInGroup_throwsStudentNotInGroupException() {
        CodePractice matchingPractice = new CodePractice();
        matchingPractice.setId(1L);

        Group group = new Group();
        group.setId(5L);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setPractice(matchingPractice);
        assignment.setGroup(group);

        User student = new User();
        student.setId(42L);
        student.setRole(User.Role.STUDENT);

        when(practiceRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(codeExecutionService.execute(anyString(), anyList(), anyInt()))
                .thenReturn(new CodeSubmissionResultDto(1, 1, "RESULT:1/1\n"));
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(42L)).thenReturn(Optional.of(student));

        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setAssignmentId(10L);
        req.setCode("public class Solution {}");

        assertThatThrownBy(() -> practiceService.submitPractice(1L, req, 42L))
                .isInstanceOf(StudentNotInGroupException.class)
                .hasMessageContaining("42")
                .hasMessageContaining("5");
    }
}

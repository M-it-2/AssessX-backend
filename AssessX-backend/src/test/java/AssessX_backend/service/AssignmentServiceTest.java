package AssessX_backend.service;

import AssessX_backend.dto.AssignmentResponseDto;
import AssessX_backend.dto.CreateAssignmentRequest;
import AssessX_backend.exception.*;
import AssessX_backend.model.*;
import AssessX_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private TestRepository testRepository;
    @Mock private CodePracticeRepository practiceRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private CodeSubmissionRepository codeSubmissionRepository;

    private AssignmentService assignmentService;
    private User teacher;
    private User student;
    private Group group;
    private Test test;
    private CodePractice practice;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
            assignmentRepository,
            groupRepository,
            testRepository,
            practiceRepository,
            userRepository,
            resultRepository,
            codeSubmissionRepository
        );

        teacher = new User();
        teacher.setId(1L);
        teacher.setGithubLogin("teacher");
        teacher.setRole(User.Role.TEACHER);

        student = new User();
        student.setId(2L);
        student.setGithubLogin("student");
        student.setRole(User.Role.STUDENT);

        group = new Group();
        group.setId(10L);
        group.setName("CS-1");

        test = new Test();
        test.setId(100L);
        test.setTitle("Java Basics");
        test.setPoints(30);

        practice = new CodePractice();
        practice.setId(200L);
        practice.setTitle("FizzBuzz");
        practice.setPoints(20);

        assignment = new Assignment();
        assignment.setId(1L);
        assignment.setGroup(group);
        assignment.setTest(test);
        assignment.setCreatedBy(teacher);
    }

    @org.junit.jupiter.api.Test
    void getAllAssignments_returnsList() {
        when(assignmentRepository.findAll()).thenReturn(List.of(assignment));
        List<AssignmentResponseDto> result = assignmentService.getAllAssignments();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getGroupId()).isEqualTo(10L);
        assertThat(result.get(0).getTestId()).isEqualTo(100L);
    }

    @org.junit.jupiter.api.Test
    void getAllAssignments_empty_returnsEmptyList() {
        when(assignmentRepository.findAll()).thenReturn(List.of());
        List<AssignmentResponseDto> result = assignmentService.getAllAssignments();
        assertThat(result).isEmpty();
    }

    @org.junit.jupiter.api.Test
    void getMyAssignments_studentWithGroup_returnsGroupAssignments() {
        student.setGroups(Set.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(assignmentRepository.findByGroupIdIn(Set.of(10L))).thenReturn(List.of(assignment));
        List<AssignmentResponseDto> result = assignmentService.getMyAssignments(2L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTestId()).isEqualTo(100L);
    }

    @org.junit.jupiter.api.Test
    void getMyAssignments_studentWithNoGroups_returnsEmpty() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        List<AssignmentResponseDto> result = assignmentService.getMyAssignments(2L);
        assertThat(result).isEmpty();
        verifyNoInteractions(assignmentRepository);
    }

    @org.junit.jupiter.api.Test
    void getMyAssignments_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assignmentService.getMyAssignments(99L))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @org.junit.jupiter.api.Test
    void createAssignment_withTest_savesAssignment() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(10L);
        req.setTestId(100L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(testRepository.findById(100L)).thenReturn(Optional.of(test));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);
        AssignmentResponseDto result = assignmentService.createAssignment(req, 1L);
        assertThat(result.getTestId()).isEqualTo(100L);
        assertThat(result.getPracticeId()).isNull();
        verify(assignmentRepository).save(argThat(a ->
            a.getTest() != null && a.getPractice() == null));
    }

    @org.junit.jupiter.api.Test
    void createAssignment_withPractice_savesAssignment() {
        Assignment practiceAssignment = new Assignment();
        practiceAssignment.setId(2L);
        practiceAssignment.setGroup(group);
        practiceAssignment.setPractice(practice);
        practiceAssignment.setCreatedBy(teacher);
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(10L);
        req.setPracticeId(200L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.findById(200L)).thenReturn(Optional.of(practice));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(practiceAssignment);
        AssignmentResponseDto result = assignmentService.createAssignment(req, 1L);
        assertThat(result.getPracticeId()).isEqualTo(200L);
        assertThat(result.getTestId()).isNull();
        verify(assignmentRepository).save(argThat(a ->
            a.getPractice() != null && a.getTest() == null));
    }

    @org.junit.jupiter.api.Test
    void createAssignment_bothTestAndPractice_throwsInvalidAssignmentException() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(10L);
        req.setTestId(100L);
        req.setPracticeId(200L);
        assertThatThrownBy(() -> assignmentService.createAssignment(req, 1L))
            .isInstanceOf(InvalidAssignmentException.class)
            .hasMessageContaining("Exactly one");
    }

    @org.junit.jupiter.api.Test
    void createAssignment_neitherTestNorPractice_throwsInvalidAssignmentException() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(10L);
        assertThatThrownBy(() -> assignmentService.createAssignment(req, 1L))
            .isInstanceOf(InvalidAssignmentException.class)
            .hasMessageContaining("Exactly one");
    }

    @org.junit.jupiter.api.Test
    void createAssignment_groupNotFound_throwsGroupNotFoundException() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(99L);
        req.setTestId(100L);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assignmentService.createAssignment(req, 1L))
            .isInstanceOf(GroupNotFoundException.class)
            .hasMessageContaining("Group not found");
    }

    @org.junit.jupiter.api.Test
    void createAssignment_testNotFound_throwsTestNotFoundException() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(10L);
        req.setTestId(99L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(testRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assignmentService.createAssignment(req, 1L))
            .isInstanceOf(TestNotFoundException.class)
            .hasMessageContaining("Test not found");
    }

    @org.junit.jupiter.api.Test
    void createAssignment_practiceNotFound_throwsCodePracticeNotFoundException() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        req.setGroupId(10L);
        req.setPracticeId(99L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(practiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assignmentService.createAssignment(req, 1L))
            .isInstanceOf(CodePracticeNotFoundException.class)
            .hasMessageContaining("Code practice not found");
    }

    @org.junit.jupiter.api.Test
    void deleteAssignment_success_callsDeleteById() {
        when(assignmentRepository.existsById(1L)).thenReturn(true);
        when(resultRepository.findByAssignmentId(1L)).thenReturn(List.of());

        assignmentService.deleteAssignment(1L);

        verify(assignmentRepository).deleteById(1L);
        verify(resultRepository).deleteByAssignmentId(1L);
        verify(codeSubmissionRepository, never()).deleteByResultId(any());
    }


    @org.junit.jupiter.api.Test
    void deleteAssignment_notFound_throwsAssignmentNotFoundException() {
        when(assignmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() ->
            assignmentService.deleteAssignment(99L)
        ).isInstanceOf(AssignmentNotFoundException.class);
    }
}

package AssessX_backend.service;

import AssessX_backend.dto.CompleteRegistrationRequest;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.exception.GroupNotFoundException;
import AssessX_backend.exception.StudentRoleRequiredException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.Group;
import AssessX_backend.model.User;
import AssessX_backend.repository.GroupRepository;
import AssessX_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;

    private AuthService authService;

    private User student;
    private User teacher;
    private Group group;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, groupRepository);

        student = new User();
        student.setId(1L);
        student.setGithubId(123L);
        student.setGithubLogin("student_user");
        student.setName("Student Name");
        student.setRole(User.Role.STUDENT);

        teacher = new User();
        teacher.setId(2L);
        teacher.setGithubId(456L);
        teacher.setGithubLogin("teacher_user");
        teacher.setName("Teacher Name");
        teacher.setRole(User.Role.TEACHER);

        group = new Group();
        group.setId(10L);
        group.setName("CS-1");
    }

    @Test
    void getCurrentUser_existingUser_returnsDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        UserResponseDto result = authService.getCurrentUser(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getGithubLogin()).isEqualTo("student_user");
        assertThat(result.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void getCurrentUser_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void completeRegistration_nameOnly_updatesUserName() {
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        authService.completeRegistration(1L, req);

        verify(userRepository).save(argThat(u -> "Updated Name".equals(u.getName())));
        verifyNoInteractions(groupRepository);
    }

    @Test
    void completeRegistration_withRoleChange_updatesRole() {
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Teacher Name");
        req.setRole("TEACHER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        authService.completeRegistration(1L, req);

        verify(userRepository).save(argThat(u -> u.getRole() == User.Role.TEACHER));
    }

    @Test
    void completeRegistration_studentWithGroup_assignsToGroup() {
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Student Name");
        req.setGroupId(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.save(any(User.class))).thenReturn(student);

        authService.completeRegistration(1L, req);

        verify(groupRepository).save(argThat(g -> g.getStudents().contains(student)));
    }

    @Test
    void completeRegistration_teacherWithGroup_throwsStudentRoleRequiredException() {
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Teacher Name");
        req.setGroupId(10L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> authService.completeRegistration(2L, req))
                .isInstanceOf(StudentRoleRequiredException.class)
                .hasMessageContaining("Only students");
    }

    @Test
    void completeRegistration_userNotFound_throwsUserNotFoundException() {
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Name");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.completeRegistration(99L, req))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void completeRegistration_groupNotFound_throwsGroupNotFoundException() {
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Student Name");
        req.setGroupId(99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.completeRegistration(1L, req))
                .isInstanceOf(GroupNotFoundException.class)
                .hasMessageContaining("Group not found");
    }
}

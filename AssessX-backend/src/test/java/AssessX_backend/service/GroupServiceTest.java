package AssessX_backend.service;

import AssessX_backend.dto.CreateGroupRequest;
import AssessX_backend.dto.GroupResponseDto;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.model.Group;
import AssessX_backend.model.User;
import AssessX_backend.exception.GroupAlreadyExistsException;
import AssessX_backend.exception.GroupNotFoundException;
import AssessX_backend.exception.StudentNotInGroupException;
import AssessX_backend.exception.StudentRoleRequiredException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.repository.GroupRepository;
import AssessX_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private Group group;
    private User student;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10L);
        student.setGithubId(222L);
        student.setGithubLogin("bob");
        student.setName("Bob");
        student.setRole(User.Role.STUDENT);
        student.setGroups(new HashSet<>());

        group = new Group();
        group.setId(1L);
        group.setName("CS-101");
        group.setStudents(new HashSet<>());
    }

    @Test
    void getAllGroups_returnsAll() {
        when(groupRepository.findAll()).thenReturn(List.of(group));

        List<GroupResponseDto> result = groupService.getAllGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("CS-101");
    }

    @Test
    void createGroup_success() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("CS-101");

        when(groupRepository.existsByName("CS-101")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        GroupResponseDto result = groupService.createGroup(req);

        assertThat(result.getName()).isEqualTo("CS-101");
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void createGroup_duplicateName_throws409() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("CS-101");

        when(groupRepository.existsByName("CS-101")).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(req))
                .isInstanceOf(GroupAlreadyExistsException.class)
                .hasMessageContaining("Group already exists");
    }

    @Test
    void getStudentsByGroupId_returnsStudents() {
        group.getStudents().add(student);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        List<UserResponseDto> result = groupService.getStudentsByGroupId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGithubLogin()).isEqualTo("bob");
    }

    @Test
    void getStudentsByGroupId_groupNotFound_throws404() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getStudentsByGroupId(99L))
                .isInstanceOf(GroupNotFoundException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void addStudentToGroup_success() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        GroupResponseDto result = groupService.addStudentToGroup(1L, 10L);

        assertThat(group.getStudents()).contains(student);
        verify(groupRepository).save(group);
    }

    @Test
    void addStudentToGroup_teacherRole_throws400() {
        student.setRole(User.Role.TEACHER);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> groupService.addStudentToGroup(1L, 10L))
                .isInstanceOf(StudentRoleRequiredException.class)
                .hasMessageContaining("Only students");
    }

    @Test
    void addStudentToGroup_userNotFound_throws404() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addStudentToGroup(1L, 99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void removeStudentFromGroup_success() {
        group.getStudents().add(student);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        groupService.removeStudentFromGroup(1L, 10L);

        assertThat(group.getStudents()).doesNotContain(student);
        verify(groupRepository).save(group);
    }

    @Test
    void removeStudentFromGroup_studentNotInGroup_throws404() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeStudentFromGroup(1L, 10L))
                .isInstanceOf(StudentNotInGroupException.class)
                .hasMessageContaining("is not in group");
    }
}

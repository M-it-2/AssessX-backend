package AssessX_backend.service;

import AssessX_backend.dto.CreateGroupRequest;
import AssessX_backend.dto.GroupResponseDto;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.exception.GroupAlreadyExistsException;
import AssessX_backend.exception.GroupNotFoundException;
import AssessX_backend.exception.StudentNotInGroupException;
import AssessX_backend.exception.StudentRoleRequiredException;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.Group;
import AssessX_backend.model.User;
import AssessX_backend.repository.GroupRepository;
import AssessX_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupResponseDto> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(GroupResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupResponseDto createGroup(CreateGroupRequest request) {
        if (groupRepository.existsByName(request.getName())) {
            throw new GroupAlreadyExistsException(request.getName());
        }
        Group group = new Group();
        group.setName(request.getName());
        return GroupResponseDto.from(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getStudentsByGroupId(Long groupId) {
        Group group = findGroupById(groupId);
        return group.getStudents().stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupResponseDto addStudentToGroup(Long groupId, Long userId) {
        Group group = findGroupById(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getRole() != User.Role.STUDENT) {
            throw new StudentRoleRequiredException("Only students can be added to a group");
        }
        group.getStudents().add(user);
        return GroupResponseDto.from(groupRepository.save(group));
    }

    @Transactional
    public void removeStudentFromGroup(Long groupId, Long userId) {
        Group group = findGroupById(groupId);
        boolean removed = group.getStudents().removeIf(u -> u.getId().equals(userId));
        if (!removed) {
            throw new StudentNotInGroupException(userId, groupId);
        }
        groupRepository.save(group);
    }

    private Group findGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }
}

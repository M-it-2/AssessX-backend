package AssessX_backend.service;

import AssessX_backend.dto.CompleteRegistrationRequest;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.model.Group;
import AssessX_backend.model.User;
import AssessX_backend.repository.GroupRepository;
import AssessX_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public AuthService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(Long userId) {
        User user = findUserById(userId);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto completeRegistration(Long userId, CompleteRegistrationRequest req) {
        User user = findUserById(userId);

        user.setName(req.getName());

        if (req.getRole() != null) {
            user.setRole(User.Role.valueOf(req.getRole()));
        }

        if (req.getGroupId() != null) {
            if (user.getRole() != User.Role.STUDENT) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Only students can be assigned to a group");
            }
            Group group = groupRepository.findById(req.getGroupId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Group not found: " + req.getGroupId()));
            group.getStudents().add(user);
            groupRepository.save(group);
        }

        user = userRepository.save(user);
        return UserResponseDto.from(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));
    }
}

package AssessX_backend.service;

import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.Group;
import AssessX_backend.model.User;
import AssessX_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setGithubId(111L);
        user.setGithubLogin("alice");
        user.setName("Alice");
        user.setRole(User.Role.STUDENT);
        user.setGroups(Set.of());
    }

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGithubLogin()).isEqualTo("alice");
    }

    @Test
    void getAllUsers_emptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    @Test
    void getUserById_found_returnsDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDto result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void getUserById_notFound_throws404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}

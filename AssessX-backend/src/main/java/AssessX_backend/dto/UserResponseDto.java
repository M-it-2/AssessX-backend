package AssessX_backend.dto;

import AssessX_backend.model.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponseDto {

    private Long id;
    private Long githubId;
    private String githubLogin;
    private String name;
    private String role;
    private LocalDateTime createdAt;
    private Set<Long> groupIds;

    public static UserResponseDto from(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.id = user.getId();
        dto.githubId = user.getGithubId();
        dto.githubLogin = user.getGithubLogin();
        dto.name = user.getName();
        dto.role = user.getRole().name();
        dto.createdAt = user.getCreatedAt();
        dto.groupIds = user.getGroups().stream()
                .map(g -> g.getId())
                .collect(Collectors.toSet());
        return dto;
    }

    public Long getId() { return id; }
    public Long getGithubId() { return githubId; }
    public String getGithubLogin() { return githubLogin; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<Long> getGroupIds() { return groupIds; }
}

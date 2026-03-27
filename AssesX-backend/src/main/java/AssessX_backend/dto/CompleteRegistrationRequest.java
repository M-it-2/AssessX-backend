package AssessX_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CompleteRegistrationRequest {

    @NotBlank(message = "Name must not be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    /** Optional: STUDENT or TEACHER. If omitted, keeps the existing role. */
    @Pattern(regexp = "STUDENT|TEACHER", message = "Role must be STUDENT or TEACHER")
    private String role;

    /** Optional: group to join. Applies only when role is STUDENT. */
    private Long groupId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
}

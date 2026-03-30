package AssessX_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupRequest {

    @NotBlank(message = "Group name must not be blank")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

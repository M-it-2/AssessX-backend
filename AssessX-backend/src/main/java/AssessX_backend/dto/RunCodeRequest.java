package AssessX_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RunCodeRequest {

    @NotBlank
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}

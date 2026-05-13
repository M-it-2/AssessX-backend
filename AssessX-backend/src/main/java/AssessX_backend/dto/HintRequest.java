package AssessX_backend.dto;

import jakarta.validation.constraints.NotNull;

public class HintRequest {

    @NotNull
    private Long assignmentId;

    private String currentCode;

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getCurrentCode() { return currentCode; }
    public void setCurrentCode(String currentCode) { this.currentCode = currentCode; }
}

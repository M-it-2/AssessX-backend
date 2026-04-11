package AssessX_backend.dto;

import tools.jackson.databind.JsonNode;

public class SubmitTestRequest {

    private JsonNode answers;
    private Long assignmentId;

    public JsonNode getAnswers() { return answers; }
    public void setAnswers(JsonNode answers) { this.answers = answers; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
}
package AssessX_backend.dto;

import AssessX_backend.model.Result;

import java.time.LocalDateTime;

public class ResultResponseDto {

    private Long id;
    private Long userId;
    private Long assignmentId;
    private Long testId;
    private Long practiceId;
    private Integer attemptNumber;
    private Integer points;
    private Integer maxPoints;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;

    public static ResultResponseDto from(Result result) {
        ResultResponseDto dto = new ResultResponseDto();
        dto.id = result.getId();
        dto.userId = result.getUser().getId();
        dto.assignmentId = result.getAssignment().getId();
        dto.testId = result.getTest() != null ? result.getTest().getId() : null;
        dto.practiceId = result.getPractice() != null ? result.getPractice().getId() : null;
        dto.attemptNumber = result.getAttemptNumber();
        dto.points = result.getPoints();
        dto.maxPoints = result.getMaxPoints();
        dto.startedAt = result.getStartedAt();
        dto.submittedAt = result.getSubmittedAt();
        dto.expiresAt = result.getExpiresAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getAssignmentId() { return assignmentId; }
    public Long getTestId() { return testId; }
    public Long getPracticeId() { return practiceId; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public Integer getPoints() { return points; }
    public Integer getMaxPoints() { return maxPoints; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}

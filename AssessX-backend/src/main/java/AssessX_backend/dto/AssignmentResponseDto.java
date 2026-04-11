package AssessX_backend.dto;

import AssessX_backend.model.Assignment;
import java.time.LocalDateTime;

public class AssignmentResponseDto {
    private Long id;
    private Long groupId;
    private String groupName;
    private Long testId;
    private String testTitle;
    private Long practiceId;
    private String practiceTitle;
    private LocalDateTime deadline;
    private Long createdById;
    private LocalDateTime createdAt;

    public static AssignmentResponseDto from(Assignment a) {
        AssignmentResponseDto dto = new AssignmentResponseDto();
        dto.id = a.getId();
        dto.groupId = a.getGroup() != null ? a.getGroup().getId() : null;
        dto.groupName = a.getGroup() != null ? a.getGroup().getName() : null;
        dto.testId = a.getTest() != null ? a.getTest().getId() : null;
        dto.testTitle = a.getTest() != null ? a.getTest().getTitle() : null;
        dto.practiceId = a.getPractice() != null ? a.getPractice().getId() : null;
        dto.practiceTitle = a.getPractice() != null ? a.getPractice().getTitle() : null;
        dto.deadline = a.getDeadline();
        dto.createdById = a.getCreatedBy() != null ? a.getCreatedBy().getId() : null;
        dto.createdAt = a.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public Long getTestId() { return testId; }
    public String getTestTitle() { return testTitle; }
    public Long getPracticeId() { return practiceId; }
    public String getPracticeTitle() { return practiceTitle; }
    public LocalDateTime getDeadline() { return deadline; }
    public Long getCreatedById() { return createdById; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

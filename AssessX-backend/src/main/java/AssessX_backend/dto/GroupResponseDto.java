package AssessX_backend.dto;

import AssessX_backend.model.Group;

import java.util.Set;
import java.util.stream.Collectors;

public class GroupResponseDto {

    private Long id;
    private String name;
    private Set<Long> studentIds;

    public static GroupResponseDto from(Group group) {
        GroupResponseDto dto = new GroupResponseDto();
        dto.id = group.getId();
        dto.name = group.getName();
        dto.studentIds = group.getStudents().stream()
                .map(u -> u.getId())
                .collect(Collectors.toSet());
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Set<Long> getStudentIds() { return studentIds; }
}

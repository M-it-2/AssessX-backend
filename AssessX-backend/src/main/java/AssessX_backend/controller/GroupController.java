package AssessX_backend.controller;

import AssessX_backend.dto.CreateGroupRequest;
import AssessX_backend.dto.GroupResponseDto;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<GroupResponseDto>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupResponseDto> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<UserResponseDto>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getStudentsByGroupId(id));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupResponseDto> addStudent(
            @PathVariable Long id,
            @RequestParam Long userId) {
        return ResponseEntity.ok(groupService.addStudentToGroup(id, userId));
    }

    @DeleteMapping("/{id}/students/{userId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> removeStudent(
            @PathVariable Long id,
            @PathVariable Long userId) {
        groupService.removeStudentFromGroup(id, userId);
        return ResponseEntity.noContent().build();
    }
}

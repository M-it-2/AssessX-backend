package AssessX_backend.controller;

import AssessX_backend.dto.ResultResponseDto;
import AssessX_backend.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<ResultResponseDto>> getMyResults(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(resultService.getMyResults(userId));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<ResultResponseDto>> getGroupResults(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(resultService.getGroupResults(groupId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultResponseDto> getResultById(@PathVariable Long id) {
        return ResponseEntity.ok(resultService.getResultById(id));
    }
}

package AssessX_backend.controller;

import AssessX_backend.dto.CompleteRegistrationRequest;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints.
 *
 * GET  /auth/github              — handled by Spring Security (redirects to GitHub OAuth)
 * GET  /auth/github/callback     — handled by Spring Security + OAuth2LoginSuccessHandler (returns JWT)
 * GET  /auth/me                  — returns current authenticated user
 * POST /auth/complete-registration — updates name/role/group after first login
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<UserResponseDto> completeRegistration(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CompleteRegistrationRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(authService.completeRegistration(userId, request));
    }
}

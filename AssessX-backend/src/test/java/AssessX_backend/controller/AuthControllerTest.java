package AssessX_backend.controller;

import AssessX_backend.dto.CompleteRegistrationRequest;
import AssessX_backend.dto.UserResponseDto;
import AssessX_backend.exception.GlobalExceptionHandler;
import AssessX_backend.exception.UserNotFoundException;
import AssessX_backend.model.User;
import AssessX_backend.service.AuthService;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private UserResponseDto studentDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        User student = new User();
        student.setId(1L);
        student.setGithubId(123L);
        student.setGithubLogin("student_user");
        student.setName("Student Name");
        student.setRole(User.Role.STUDENT);
        studentDto = UserResponseDto.from(student);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        authenticateAs(subject, null);
    }

    private void authenticateAs(String subject, String role) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject);
        if (role != null) {
            builder.claim("role", role);
        }
        Jwt jwt = builder.build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMe_withJwt_returnsCurrentUser() throws Exception {
        authenticateAs("1");
        when(authService.getCurrentUser(1L)).thenReturn(studentDto);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.githubLogin").value("student_user"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void getMe_userNotFound_returns404() throws Exception {
        authenticateAs("99");
        when(authService.getCurrentUser(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void completeRegistration_validRequest_returnsOk() throws Exception {
        authenticateAs("1", "STUDENT");
        CompleteRegistrationRequest req = new CompleteRegistrationRequest();
        req.setName("Updated Name");
        req.setRole("STUDENT");

        when(authService.completeRegistration(eq(1L), any())).thenReturn(studentDto);

        mockMvc.perform(post("/auth/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubLogin").value("student_user"));
    }

    @Test
    void completeRegistration_missingName_returns400() throws Exception {
        authenticateAs("1");
        String body = "{\"role\":\"STUDENT\"}";

        mockMvc.perform(post("/auth/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void completeRegistration_emptyBody_returns400() throws Exception {
        authenticateAs("1");

        mockMvc.perform(post("/auth/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
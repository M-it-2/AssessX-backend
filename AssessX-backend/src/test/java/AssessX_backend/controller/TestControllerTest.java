package AssessX_backend.controller;

import AssessX_backend.dto.CreateTestRequest;
import AssessX_backend.dto.SubmitTestRequest;
import AssessX_backend.dto.TestResponseDto;
import AssessX_backend.dto.TestSubmitResultDto;
import AssessX_backend.exception.GlobalExceptionHandler;
import AssessX_backend.exception.TestNotFoundException;
import AssessX_backend.model.Test;
import AssessX_backend.service.TestService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TestControllerTest {

    @Mock TestService testService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private TestResponseDto testDto;
    private CreateTestRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController(testService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

        Test test = new Test();
        test.setId(1L);
        test.setTitle("Java Basics");
        test.setPoints(30);
        test.setTimeLimitSec(600);
        test.setQuestions("{\"q1\":\"What is JVM?\"}");
        test.setAnswers("{\"q1\":\"Java Virtual Machine\"}");

        testDto = TestResponseDto.from(test, true);

        ObjectNode questions = objectMapper.valueToTree(Map.of("q1", "What is JVM?"));
        ObjectNode answers = objectMapper.valueToTree(Map.of("q1", "Java Virtual Machine"));

        validCreateRequest = new CreateTestRequest();
        validCreateRequest.setTitle("Java Basics");
        validCreateRequest.setQuestions(questions);
        validCreateRequest.setAnswers(answers);
        validCreateRequest.setPoints(30);
        validCreateRequest.setTimeLimitSec(600);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject, String role) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject(subject);
        if (role != null) {
            builder.claim("role", role);
        }
        Jwt jwt = builder.build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, Collections.emptyList()));
    }

    @org.junit.jupiter.api.Test
    void getAllTests_returnsListOfTests() throws Exception {
        authenticateAs("1", "STUDENT");
        when(testService.getAllTests()).thenReturn(List.of(testDto));

        mockMvc.perform(get("/api/tests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].title").value("Java Basics"))
            .andExpect(jsonPath("$[0].points").value(30));
    }

    @org.junit.jupiter.api.Test
    void getAllTests_emptyList_returnsEmptyArray() throws Exception {
        authenticateAs("1", "STUDENT");
        when(testService.getAllTests()).thenReturn(List.of());

        mockMvc.perform(get("/api/tests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @org.junit.jupiter.api.Test
    void getTestById_asTeacher_returnsTestDto() throws Exception {
        authenticateAs("2", "TEACHER");
        when(testService.getTestById(1L, "TEACHER")).thenReturn(testDto);

        mockMvc.perform(get("/api/tests/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Java Basics"))
            .andExpect(jsonPath("$.answers").exists());
    }

    @org.junit.jupiter.api.Test
    void getTestById_asStudent_returnsTestDtoWithoutAnswers() throws Exception {
        authenticateAs("1", "STUDENT");

        Test testEntity = new Test();
        testEntity.setId(1L);
        testEntity.setTitle("Java Basics");
        testEntity.setQuestions("[{\"id\":\"1\"}]");
        testEntity.setPoints(30);
        testEntity.setTimeLimitSec(600);

        TestResponseDto studentView = TestResponseDto.from(testEntity, false);
        when(testService.getTestById(1L, "STUDENT")).thenReturn(studentView);

        mockMvc.perform(get("/api/tests/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.answers").doesNotExist());
    }

    @org.junit.jupiter.api.Test
    void getTestById_notFound_returns404() throws Exception {
        authenticateAs("1", "STUDENT");
        when(testService.getTestById(eq(99L), any())).thenThrow(new TestNotFoundException(99L));

        mockMvc.perform(get("/api/tests/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.status").value(404));
    }

    @org.junit.jupiter.api.Test
    void createTest_validRequest_returns201() throws Exception {
        authenticateAs("2", "TEACHER");
        when(testService.createTest(any(), eq(2L))).thenReturn(testDto);

        mockMvc.perform(post("/api/tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @org.junit.jupiter.api.Test
    void createTest_missingTitle_returns400() throws Exception {
        authenticateAs("2", "TEACHER");
        String body = "{\"questions\":{},\"answers\":{},\"points\":10,\"timeLimitSec\":60}";

        mockMvc.perform(post("/api/tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.status").value(400));
    }

    @org.junit.jupiter.api.Test
    void createTest_missingPoints_returns400() throws Exception {
        authenticateAs("2", "TEACHER");
        String body = "{\"title\":\"T\",\"questions\":{},\"answers\":{},\"timeLimitSec\":60}";

        mockMvc.perform(post("/api/tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    void updateTest_validRequest_returns200() throws Exception {
        authenticateAs("2", "TEACHER");
        when(testService.updateTest(eq(1L), any())).thenReturn(testDto);

        mockMvc.perform(put("/api/tests/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @org.junit.jupiter.api.Test
    void deleteTest_existingTest_returns204() throws Exception {
        authenticateAs("2", "TEACHER");
        doNothing().when(testService).deleteTest(1L);

        mockMvc.perform(delete("/api/tests/1"))
            .andExpect(status().isNoContent());
    }

    @org.junit.jupiter.api.Test
    void deleteTest_notFound_returns404() throws Exception {
        authenticateAs("2", "TEACHER");
        doThrow(new TestNotFoundException(99L)).when(testService).deleteTest(99L);

        mockMvc.perform(delete("/api/tests/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }

    @org.junit.jupiter.api.Test
    void submitTest_validRequest_returns200() throws Exception {
        authenticateAs("1", "STUDENT");
        SubmitTestRequest req = new SubmitTestRequest();
        req.setAnswers(objectMapper.readTree("{\"q1\":\"Java Virtual Machine\"}"));

        TestSubmitResultDto result = new TestSubmitResultDto(30, 30, 1, 1);
        when(testService.submitTest(eq(1L), any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/tests/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.earnedPoints").value(30))
            .andExpect(jsonPath("$.maxPoints").value(30))
            .andExpect(jsonPath("$.correctAnswers").value(1))
            .andExpect(jsonPath("$.totalQuestions").value(1));
    }

    @org.junit.jupiter.api.Test
    void submitTest_nullAnswers_returns400() throws Exception {
        authenticateAs("1", "STUDENT");

        mockMvc.perform(post("/api/tests/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}

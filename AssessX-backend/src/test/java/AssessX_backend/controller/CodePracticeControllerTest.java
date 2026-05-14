package AssessX_backend.controller;

import AssessX_backend.dto.CodePracticeResponseDto;
import AssessX_backend.dto.CodeSubmissionResultDto;
import AssessX_backend.dto.CreateCodePracticeRequest;
import AssessX_backend.dto.CsvImportResultDto;
import AssessX_backend.dto.HintRequest;
import AssessX_backend.dto.HintResponseDto;
import AssessX_backend.dto.SubmitCodeRequest;
import AssessX_backend.exception.CodePracticeNotFoundException;
import AssessX_backend.exception.GlobalExceptionHandler;
import AssessX_backend.exception.HintAlreadyUsedException;
import AssessX_backend.exception.OllamaUnavailableException;
import AssessX_backend.model.CodePractice;
import AssessX_backend.service.CodePracticeService;
import AssessX_backend.service.HintService;

import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@ExtendWith(MockitoExtension.class)
class CodePracticeControllerTest {

    @Mock CodePracticeService practiceService;
    @Mock HintService hintService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private CodePracticeResponseDto practiceDto;
    private CreateCodePracticeRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CodePracticeController(practiceService, hintService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        CodePractice practice = new CodePractice();
        practice.setId(1L);
        practice.setTitle("FizzBuzz");
        practice.setDescription("Implement FizzBuzz");
        practice.setPoints(20);
        practice.setTimeLimitSec(30);
        practiceDto = CodePracticeResponseDto.from(practice);

        validCreateRequest = new CreateCodePracticeRequest();
        validCreateRequest.setTitle("FizzBuzz");
        validCreateRequest.setDescription("Implement FizzBuzz");
        validCreateRequest.setUnitTests(List.of("assert new Solution().fizzBuzz(3).equals(\"Fizz\");"));
        validCreateRequest.setPoints(20);
        validCreateRequest.setTimeLimitSec(30);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, Collections.emptyList()));
    }

    @org.junit.jupiter.api.Test
    void getAllPractices_returnsListOfPractices() throws Exception {
        authenticateAs("1");
        when(practiceService.getAllPractices()).thenReturn(List.of(practiceDto));

        mockMvc.perform(get("/api/practices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("FizzBuzz"))
                .andExpect(jsonPath("$[0].points").value(20));
    }

    @org.junit.jupiter.api.Test
    void getAllPractices_emptyList_returnsEmptyArray() throws Exception {
        authenticateAs("1");
        when(practiceService.getAllPractices()).thenReturn(List.of());

        mockMvc.perform(get("/api/practices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @org.junit.jupiter.api.Test
    void getPracticeById_existingId_returnsPractice() throws Exception {
        authenticateAs("1");
        when(practiceService.getPracticeById(1L)).thenReturn(practiceDto);

        mockMvc.perform(get("/api/practices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("FizzBuzz"))
                .andExpect(jsonPath("$.description").value("Implement FizzBuzz"));
    }

    @org.junit.jupiter.api.Test
    void getPracticeById_notFound_returns404() throws Exception {
        authenticateAs("1");
        when(practiceService.getPracticeById(99L)).thenThrow(new CodePracticeNotFoundException(99L));

        mockMvc.perform(get("/api/practices/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @org.junit.jupiter.api.Test
    void createPractice_validRequest_returns201() throws Exception {
        authenticateAs("2");
        when(practiceService.createPractice(any(), eq(2L))).thenReturn(practiceDto);

        mockMvc.perform(post("/api/practices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("FizzBuzz"));
    }

    @org.junit.jupiter.api.Test
    void createPractice_missingTitle_returns400() throws Exception {
        authenticateAs("2");
        String body = "{\"description\":\"desc\",\"points\":10,\"timeLimitSec\":30}";

        mockMvc.perform(post("/api/practices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @org.junit.jupiter.api.Test
    void createPractice_missingDescription_returns400() throws Exception {
        authenticateAs("2");
        String body = "{\"title\":\"T\",\"points\":10,\"timeLimitSec\":30}";

        mockMvc.perform(post("/api/practices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    void updatePractice_validRequest_returns200() throws Exception {
        authenticateAs("2");
        when(practiceService.updatePractice(eq(1L), any())).thenReturn(practiceDto);

        mockMvc.perform(put("/api/practices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("FizzBuzz"));
    }

    @org.junit.jupiter.api.Test
    void deletePractice_existingId_returns204() throws Exception {
        authenticateAs("2");
        doNothing().when(practiceService).deletePractice(1L);

        mockMvc.perform(delete("/api/practices/1"))
                .andExpect(status().isNoContent());
    }

    @org.junit.jupiter.api.Test
    void deletePractice_notFound_returns404() throws Exception {
        authenticateAs("2");
        doThrow(new CodePracticeNotFoundException(99L)).when(practiceService).deletePractice(99L);

        mockMvc.perform(delete("/api/practices/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @org.junit.jupiter.api.Test
    void submitPractice_validRequest_returns200() throws Exception {
        authenticateAs("1");
        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setCode("public class Solution { public String fizzBuzz(int n) { return \"Fizz\"; } }");

        CodeSubmissionResultDto result = new CodeSubmissionResultDto(3, 3, "All tests passed");
        when(practiceService.submitPractice(eq(1L), any(), eq(1L))).thenReturn(result);

        mockMvc.perform(post("/api/practices/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passedTests").value(3))
                .andExpect(jsonPath("$.totalTests").value(3))
                .andExpect(jsonPath("$.output").value("All tests passed"));
    }

    @org.junit.jupiter.api.Test
    void submitPractice_missingCode_returns400() throws Exception {
        authenticateAs("1");

        mockMvc.perform(post("/api/practices/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @org.junit.jupiter.api.Test
    void submitPractice_blankCode_returns400() throws Exception {
        authenticateAs("1");
        String body = "{\"code\":\"\"}";

        mockMvc.perform(post("/api/practices/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    void requestHint_validRequest_returns200WithHint() throws Exception {
        authenticateAs("1");
        HintRequest req = new HintRequest();
        req.setAssignmentId(5L);
        req.setCurrentCode("public class Solution {}");

        HintResponseDto dto = new HintResponseDto("Think about the + operator.", java.time.LocalDateTime.now());
        when(hintService.requestHint(eq(1L), eq(5L), eq("public class Solution {}"), eq(1L))).thenReturn(dto);

        mockMvc.perform(post("/api/practices/1/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hint").value("Think about the + operator."))
                .andExpect(jsonPath("$.hintUsedAt").exists());
    }

    @org.junit.jupiter.api.Test
    void requestHint_missingAssignmentId_returns400() throws Exception {
        authenticateAs("1");
        String body = "{\"currentCode\":\"public class Solution {}\"}";

        mockMvc.perform(post("/api/practices/1/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @org.junit.jupiter.api.Test
    void requestHint_alreadyUsed_returns409() throws Exception {
        authenticateAs("1");
        HintRequest req = new HintRequest();
        req.setAssignmentId(5L);

        when(hintService.requestHint(eq(1L), eq(5L), isNull(), eq(1L)))
                .thenThrow(new HintAlreadyUsedException(5L));

        mockMvc.perform(post("/api/practices/1/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @org.junit.jupiter.api.Test
    void requestHint_ollamaUnavailable_returns503() throws Exception {
        authenticateAs("1");
        HintRequest req = new HintRequest();
        req.setAssignmentId(5L);

        when(hintService.requestHint(eq(1L), eq(5L), isNull(), eq(1L)))
                .thenThrow(new OllamaUnavailableException());

        mockMvc.perform(post("/api/practices/1/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @org.junit.jupiter.api.Test
    void importFromCsv_validFile_returns201WithSummary() throws Exception {
        authenticateAs("1");
        String csvContent = "task_name,task_description,max_score,test_class_name,test_method_name,test_code\n" +
                "FizzBuzz,Implement FizzBuzz,20,SolutionTest,testFizz,assert true;\n";
        MockMultipartFile file = new MockMultipartFile("file", "practices.csv", "text/csv", csvContent.getBytes());

        CsvImportResultDto result = new CsvImportResultDto(1, 1, List.of());
        when(practiceService.importFromCsv(any(), eq(1L))).thenReturn(result);

        mockMvc.perform(multipart("/api/practices/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdPractices").value(1))
                .andExpect(jsonPath("$.addedTests").value(1))
                .andExpect(jsonPath("$.failedRows").isArray());
    }

    @org.junit.jupiter.api.Test
    void importFromCsv_emptyFile_returns400() throws Exception {
        authenticateAs("1");
        MockMultipartFile file = new MockMultipartFile("file", "practices.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/practices/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
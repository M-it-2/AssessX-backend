package AssessX_backend.service;

import AssessX_backend.dto.CodeSubmissionResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeExecutionServiceTest {

    private CodeExecutionService codeExecutionService;

    @BeforeEach
    void setUp() {
        codeExecutionService = new CodeExecutionService();
        ReflectionTestUtils.setField(codeExecutionService, "sandboxVolume", "assessx_sandbox");
        ReflectionTestUtils.setField(codeExecutionService, "sandboxPath",
                System.getProperty("java.io.tmpdir") + "/assessx-test");
    }

    @Test
    void execute_emptyUnitTests_returnsNoTestsDefined() {
        CodeSubmissionResultDto result = codeExecutionService.execute(
                "public class Solution {}", List.of(), 10);

        assertThat(result.getPassedTests()).isZero();
        assertThat(result.getTotalTests()).isZero();
        assertThat(result.getOutput()).isEqualTo("No unit tests defined");
    }

    @Test
    void execute_invalidSandboxPath_returnsExecutionError() throws Exception {
        // Use an existing regular file as sandbox path so createDirectories fails
        Path tempFile = Files.createTempFile("assessx-blocked", ".tmp");
        try {
            ReflectionTestUtils.setField(codeExecutionService, "sandboxPath", tempFile.toString());

            CodeSubmissionResultDto result = codeExecutionService.execute(
                    "public class Solution {}", List.of("assert true;"), 5);

            assertThat(result.getPassedTests()).isZero();
            assertThat(result.getTotalTests()).isEqualTo(1);
            assertThat(result.getOutput()).startsWith("Execution error");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void buildRunner_singleTest_containsRequiredStructure() throws Exception {
        Method buildRunner = CodeExecutionService.class.getDeclaredMethod("buildRunner", List.class);
        buildRunner.setAccessible(true);

        String runner = (String) buildRunner.invoke(codeExecutionService, List.of("assert 1 == 1;"));

        assertThat(runner).contains("public class Runner");
        assertThat(runner).contains("total = 1");
        assertThat(runner).contains("passed++");
        assertThat(runner).contains("RESULT:");
        assertThat(runner).contains("assert 1 == 1;");
    }

    @Test
    void buildRunner_multipleTests_containsCorrectTotalAndAllTests() throws Exception {
        Method buildRunner = CodeExecutionService.class.getDeclaredMethod("buildRunner", List.class);
        buildRunner.setAccessible(true);

        String runner = (String) buildRunner.invoke(codeExecutionService,
                List.of("assert 1 == 1;", "assert 2 == 2;", "assert 3 == 3;"));

        assertThat(runner).contains("total = 3");
        assertThat(runner).contains("Test 1");
        assertThat(runner).contains("Test 2");
        assertThat(runner).contains("Test 3");
        assertThat(runner).contains("assert 1 == 1;");
        assertThat(runner).contains("assert 3 == 3;");
    }

    @Test
    void buildRunner_testWithException_catchesThrowable() throws Exception {
        Method buildRunner = CodeExecutionService.class.getDeclaredMethod("buildRunner", List.class);
        buildRunner.setAccessible(true);

        String runner = (String) buildRunner.invoke(codeExecutionService, List.of("assert false;"));

        assertThat(runner).contains("catch (Throwable t)");
        assertThat(runner).contains("FAILED");
    }

    @Test
    void parsePassedCount_validResultLine_returnsCorrectCount() throws Exception {
        Method parsePassedCount = CodeExecutionService.class.getDeclaredMethod("parsePassedCount", String.class);
        parsePassedCount.setAccessible(true);

        int result = (int) parsePassedCount.invoke(codeExecutionService,
                "Test 2 FAILED: expected 1 got 2\nRESULT:4/5\n");

        assertThat(result).isEqualTo(4);
    }

    @Test
    void parsePassedCount_allTestsPassed_returnsTotal() throws Exception {
        Method parsePassedCount = CodeExecutionService.class.getDeclaredMethod("parsePassedCount", String.class);
        parsePassedCount.setAccessible(true);

        int result = (int) parsePassedCount.invoke(codeExecutionService, "RESULT:3/3");

        assertThat(result).isEqualTo(3);
    }

    @Test
    void parsePassedCount_noResultLine_returnsZero() throws Exception {
        Method parsePassedCount = CodeExecutionService.class.getDeclaredMethod("parsePassedCount", String.class);
        parsePassedCount.setAccessible(true);

        int result = (int) parsePassedCount.invoke(codeExecutionService,
                "error: cannot find symbol\nSolution.java:1: error");

        assertThat(result).isZero();
    }

    @Test
    void parsePassedCount_malformedResultLine_returnsZero() throws Exception {
        Method parsePassedCount = CodeExecutionService.class.getDeclaredMethod("parsePassedCount", String.class);
        parsePassedCount.setAccessible(true);

        int result = (int) parsePassedCount.invoke(codeExecutionService, "RESULT:invalid");

        assertThat(result).isZero();
    }
}

package AssessX_backend.dto;

import java.util.List;

public class RunCodeResultDto {

    private final boolean compiled;
    private final List<TestRunResult> results;
    private final int passedCount;
    private final int totalCount;

    public RunCodeResultDto(boolean compiled, List<TestRunResult> results, int passedCount, int totalCount) {
        this.compiled = compiled;
        this.results = results;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
    }

    public boolean isCompiled() { return compiled; }
    public List<TestRunResult> getResults() { return results; }
    public int getPassedCount() { return passedCount; }
    public int getTotalCount() { return totalCount; }

    public static class TestRunResult {
        private final String testName;
        private final boolean passed;
        private final String error;

        public TestRunResult(String testName, boolean passed, String error) {
            this.testName = testName;
            this.passed = passed;
            this.error = error;
        }

        public String getTestName() { return testName; }
        public boolean isPassed() { return passed; }
        public String getError() { return error; }
    }
}

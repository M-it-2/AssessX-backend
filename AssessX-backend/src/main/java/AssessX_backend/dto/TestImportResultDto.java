package AssessX_backend.dto;

import java.util.List;

public class TestImportResultDto {

    private final int createdTests;
    private final int updatedTests;
    private final List<String> failedRows;

    public TestImportResultDto(int createdTests, int updatedTests, List<String> failedRows) {
        this.createdTests = createdTests;
        this.updatedTests = updatedTests;
        this.failedRows = failedRows;
    }

    public int getCreatedTests() { return createdTests; }
    public int getUpdatedTests() { return updatedTests; }
    public List<String> getFailedRows() { return failedRows; }
}

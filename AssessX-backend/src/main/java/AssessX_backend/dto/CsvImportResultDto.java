package AssessX_backend.dto;

import java.util.List;

public class CsvImportResultDto {

    private final int createdPractices;
    private final int addedTests;
    private final List<String> failedRows;

    public CsvImportResultDto(int createdPractices, int addedTests, List<String> failedRows) {
        this.createdPractices = createdPractices;
        this.addedTests = addedTests;
        this.failedRows = failedRows;
    }

    public int getCreatedPractices() { return createdPractices; }
    public int getAddedTests() { return addedTests; }
    public List<String> getFailedRows() { return failedRows; }
}

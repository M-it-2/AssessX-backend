package AssessX_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_submissions")
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private Result result;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "test_output", columnDefinition = "TEXT")
    private String testOutput;

    @Column(name = "passed_tests")
    private Integer passedTests = 0;

    @Column(name = "total_tests")
    private Integer totalTests = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTestOutput() { return testOutput; }
    public void setTestOutput(String testOutput) { this.testOutput = testOutput; }

    public Integer getPassedTests() { return passedTests; }
    public void setPassedTests(Integer passedTests) { this.passedTests = passedTests; }

    public Integer getTotalTests() { return totalTests; }
    public void setTotalTests(Integer totalTests) { this.totalTests = totalTests; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}

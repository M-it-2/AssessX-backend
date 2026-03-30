package AssessX_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "practice_unit_tests")
public class PracticeUnitTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_id", nullable = false)
    private CodePractice practice;

    @Column(name = "test_code", nullable = false, columnDefinition = "TEXT")
    private String testCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CodePractice getPractice() { return practice; }
    public void setPractice(CodePractice practice) { this.practice = practice; }

    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }
}

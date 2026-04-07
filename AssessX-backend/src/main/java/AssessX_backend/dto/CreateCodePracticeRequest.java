package AssessX_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateCodePracticeRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @NotEmpty
    private List<String> unitTests;

    @NotNull
    @Min(1)
    private Integer points;

    @NotNull
    @Min(1)
    private Integer timeLimitSec;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getUnitTests() { return unitTests; }
    public void setUnitTests(List<String> unitTests) { this.unitTests = unitTests; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getTimeLimitSec() { return timeLimitSec; }
    public void setTimeLimitSec(Integer timeLimitSec) { this.timeLimitSec = timeLimitSec; }
}

package AssessX_backend.dto;

import AssessX_backend.model.Test;

public class TestResponseDto {
    private Long id;
    private String title;
    private String questions;
    private String answers;
    private Integer points;
    private Integer timeLimitSec;

    public static TestResponseDto from(Test test, boolean includeAnswers) {
        TestResponseDto dto = new TestResponseDto();
        dto.setId(test.getId());
        dto.setTitle(test.getTitle());
        dto.setQuestions(test.getQuestions().toString());
        if (includeAnswers) {
            dto.setAnswers(test.getAnswers().toString());
        } else {
            dto.setAnswers(null);
        }
        dto.setPoints(test.getPoints());
        dto.setTimeLimitSec(test.getTimeLimitSec());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getQuestions() { return questions; }
    public void setQuestions(String questions) { this.questions = questions; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Integer getTimeLimitSec() { return timeLimitSec; }
    public void setTimeLimitSec(Integer timeLimitSec) { this.timeLimitSec = timeLimitSec; }
}
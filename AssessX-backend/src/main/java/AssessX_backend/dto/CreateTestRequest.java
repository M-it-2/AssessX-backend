package AssessX_backend.dto;

import tools.jackson.databind.JsonNode;

public class CreateTestRequest {
    private String title;
    private JsonNode questions;
    private JsonNode answers;
    private Integer points;
    private Integer timeLimitSec;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonNode getQuestions() {
        return questions;
    }

    public void setQuestions(JsonNode questions) {
        this.questions = questions;
    }

    public JsonNode getAnswers() {
        return answers;
    }

    public void setAnswers(JsonNode answers) {
        this.answers = answers;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTimeLimitSec() {
        return timeLimitSec;
    }

    public void setTimeLimitSec(Integer timeLimitSec) {
        this.timeLimitSec = timeLimitSec;
    }
}
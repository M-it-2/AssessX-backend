package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class TestAnswersParseException extends AppException {

    public TestAnswersParseException(Long testId) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse answers for test: " + testId);
    }
}

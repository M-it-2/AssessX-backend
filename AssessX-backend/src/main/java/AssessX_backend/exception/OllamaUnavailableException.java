package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class OllamaUnavailableException extends AppException {

    public OllamaUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI hint service unavailable");
    }
}

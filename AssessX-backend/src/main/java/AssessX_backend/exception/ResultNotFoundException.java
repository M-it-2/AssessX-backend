package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class ResultNotFoundException extends AppException {

    public ResultNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Result not found: " + id);
    }
}

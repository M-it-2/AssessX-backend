package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class TestNotFoundException extends AppException {

    public TestNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Test not found: " + id);
    }
}

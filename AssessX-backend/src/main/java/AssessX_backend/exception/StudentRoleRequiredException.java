package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class StudentRoleRequiredException extends AppException {

    public StudentRoleRequiredException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

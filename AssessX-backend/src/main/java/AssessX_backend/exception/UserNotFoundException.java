package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {

    public UserNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "User not found: " + id);
    }
}

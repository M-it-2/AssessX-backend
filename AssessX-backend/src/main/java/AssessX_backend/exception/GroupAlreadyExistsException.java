package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class GroupAlreadyExistsException extends AppException {

    public GroupAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "Group already exists: " + name);
    }
}

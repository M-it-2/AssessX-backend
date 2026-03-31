package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class GroupNotFoundException extends AppException {

    public GroupNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Group not found: " + id);
    }
}

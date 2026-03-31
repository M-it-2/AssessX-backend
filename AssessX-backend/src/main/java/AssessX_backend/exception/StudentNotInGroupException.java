package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class StudentNotInGroupException extends AppException {

    public StudentNotInGroupException(Long userId, Long groupId) {
        super(HttpStatus.NOT_FOUND, "Student " + userId + " is not in group " + groupId);
    }
}

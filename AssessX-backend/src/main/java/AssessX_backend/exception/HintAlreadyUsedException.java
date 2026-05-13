package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class HintAlreadyUsedException extends AppException {

    public HintAlreadyUsedException(Long assignmentId) {
        super(HttpStatus.CONFLICT, "Hint already used for assignment: " + assignmentId);
    }
}

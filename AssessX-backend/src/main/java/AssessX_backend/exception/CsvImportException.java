package AssessX_backend.exception;

import org.springframework.http.HttpStatus;

public class CsvImportException extends AppException {

    public CsvImportException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

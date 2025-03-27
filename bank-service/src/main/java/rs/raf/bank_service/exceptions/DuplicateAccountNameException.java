package rs.raf.bank_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DuplicateAccountNameException extends RuntimeException {

    public DuplicateAccountNameException(String message) {
        super(message);
    }
}

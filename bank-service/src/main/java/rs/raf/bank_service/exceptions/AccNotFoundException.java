package rs.raf.bank_service.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccNotFoundException extends RuntimeException {
    public AccNotFoundException(String message) {
        super(message);
    }
}


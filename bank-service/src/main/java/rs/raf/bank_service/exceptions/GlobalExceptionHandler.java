package rs.raf.bank_service.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rs.raf.bank_service.domain.dto.ErrorMessageDto;

import java.util.Optional;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Obrada grešaka vezanih za validaciju (npr. @Valid anotacija)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Validation error");
        logger.error("Validation error: {}", errorMessage);
        return ResponseEntity.badRequest().body(new ErrorMessageDto(errorMessage));
    }

    // Obrada grešaka koje označavaju da resurs nije pronađen (npr. PayeeNotFound, LoanRequestNotFound, AccountNotFound)
    @ExceptionHandler({PayeeNotFoundException.class, LoanRequestNotFoundException.class, AccountNotFoundException.class})
    public ResponseEntity<ErrorMessageDto> handleNotFoundExceptions(RuntimeException ex) {
        logger.error("Not found error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageDto(ex.getMessage()));
    }

    // Obrada grešaka koje označavaju loš zahtev (npr. CurrencyNotFound, InvalidLimit)
    @ExceptionHandler({CurrencyNotFoundException.class, InvalidLimitException.class})
    public ResponseEntity<ErrorMessageDto> handleBadRequestExceptions(RuntimeException ex) {
        logger.error("Bad request error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(ex.getMessage()));
    }

    // Generička obrada svih ostalih izuzetaka
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageDto> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessageDto("An unexpected error occurred: " + ex.getMessage()));
    }
}

package rs.raf.bank_service.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rs.raf.bank_service.domain.dto.ErrorMessageDto;


@Order(Ordered.HIGHEST_PRECEDENCE)  // ✅ Ovo osigurava da Spring prvo koristi naš handler
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        logger.error(errorMessage);
        return ResponseEntity.badRequest().body(new ErrorMessageDto(errorMessage));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({PayeeNotFoundException.class, LoanRequestNotFoundException.class})
    public ResponseEntity<ErrorMessageDto> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageDto(ex.getMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({AccountNotFoundException.class, CurrencyNotFoundException.class})
    public ResponseEntity<ErrorMessageDto> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageDto(ex.getMessage()));

    }
}

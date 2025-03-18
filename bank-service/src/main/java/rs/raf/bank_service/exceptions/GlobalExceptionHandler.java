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

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleAccountNotFoundException(AccountNotFoundException e) {
        return new ResponseEntity<>(new ErrorMessageDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleCardNotFoundException(CardNotFoundException e) {
        return new ResponseEntity<>(new ErrorMessageDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleClientNotFoundException(ClientNotFoundException e) {
        return new ResponseEntity<>(new ErrorMessageDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorMessageDto> handleUnauthorizedException(UnauthorizedException e) {
        return new ResponseEntity<>(new ErrorMessageDto(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageDto> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(new ErrorMessageDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageDto> handleGeneralException(Exception e) {
        return new ResponseEntity<>(new ErrorMessageDto("Internal Server Error: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

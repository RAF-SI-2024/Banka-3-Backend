package rs.raf.user_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rs.raf.user_service.dto.ErrorMessageDto;
import rs.raf.user_service.dto.ValidationErrorMessageDto;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorMessageDto> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ValidationErrorMessageDto(errors));
    }

//    @ExceptionHandler(EmailAlreadyExistsException.class)
//    public ResponseEntity<ErrorMessageDto> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(ex.getMessage()));
//    }

//    @ExceptionHandler({EmailAlreadyExistsException.class, UserAlreadyExistsException.class})
//    public ResponseEntity<ErrorMessageDto> handleUserAlreadyExistsException(RuntimeException ex) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(ex.getMessage()));
//    }
}
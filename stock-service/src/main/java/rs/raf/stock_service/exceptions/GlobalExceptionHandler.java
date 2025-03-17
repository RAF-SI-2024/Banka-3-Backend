package rs.raf.stock_service.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rs.raf.stock_service.domain.dto.ErrorMessageDto;


@Order(Ordered.HIGHEST_PRECEDENCE)
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


    // dodavati exceptione ovde

//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ExceptionHandler({PayeeNotFoundException.class, LoanRequestNotFoundException.class})
//    public ResponseEntity<ErrorMessageDto> handleNotFound(RuntimeException ex) {
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageDto(ex.getMessage()));
//    }

    }

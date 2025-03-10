package rs.raf.bank_service.exceptions;



import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Order(Ordered.HIGHEST_PRECEDENCE)  // ✅ Ovo osigurava da Spring prvo koristi naš handler
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
//        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
//        logger.error(errorMessage);
//        return ResponseEntity.badRequest().body(errorMessage);
//    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("message", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)  // ✅ Forsiramo JSON odgovor
                .body(response);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PayeeNotFoundException.class)
    public ResponseEntity<String> handlePayeeNotFoundException(PayeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
    /**
     * Obrada slučaja kada traženi kurs nije pronađen.
     */
    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleExchangeRateNotFoundException(ExchangeRateNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Exchange rate not found");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Obrada slučaja kada je unos podataka nevalidan.
     */
    @ExceptionHandler(InvalidExchangeRateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidExchangeRateException(InvalidExchangeRateException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid exchange rate input");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    /**
     * Generički handler za nepoznate greške.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal server error");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }



    }

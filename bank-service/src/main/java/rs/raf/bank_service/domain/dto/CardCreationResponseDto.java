package rs.raf.bank_service.domain.dto;

import java.time.LocalDate;

public class CardCreationResponseDto {
    private String message;
    private boolean success;
    private String cardNumber;
    private LocalDate expirationDate;
}

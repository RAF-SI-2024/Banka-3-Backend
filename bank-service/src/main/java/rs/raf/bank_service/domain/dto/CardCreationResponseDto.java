package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardCreationResponseDto {
    private String message;
    private boolean success;
    private String cardNumber;
    private LocalDate expirationDate;
}

package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterBankTransactionResponse {

    private boolean ready;              // true = spremni za commit, false = odbijeno
    private String message;             // poruka o uspehu/neuspehu

    private BigDecimal finalAmount;     // koliko se uplacuje nakon provizije
    private String finalCurrency;       // valuta koja se uplacuje

    private BigDecimal exchangeRate;    // kurs koji je primenjen
    private BigDecimal fee;             // iznos provizije
}

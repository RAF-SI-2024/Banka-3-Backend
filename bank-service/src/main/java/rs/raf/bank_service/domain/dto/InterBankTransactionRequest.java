package rs.raf.bank_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InterBankTransactionRequest {

    private String fromAccountNumber;     // Racun posiljaoca
    private String fromCurrencyId;        // Valuta sa koje se salje (npr. "RSD")

    private String toAccountNumber;       // Racun primaoca
    private String toCurrencyId;          // Valuta primaoca (npr. "EUR")

    private BigDecimal amount;            // Iznos
    private String codeId;                // Sifra placanja (npr. "289")

    private String referenceNumber;       // Poziv na broj
    private String purpose;               // Svrha placanja
}

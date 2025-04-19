package rs.raf.bank_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.ExchangeRateClient;
import rs.raf.bank_service.domain.dto.InterBankTransactionRequest;
import rs.raf.bank_service.domain.dto.InterBankTransactionResponse;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.enums.AccountStatus;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterBankTransactionService {

    private final AccountRepository accountRepository;
    private final ExchangeRateClient exchangeRateClient;
    @Autowired
    private final ObjectMapper objectMapper;

    public InterBankTransactionResponse prepare(InterBankTransactionRequest request) {
        Optional<Account> recipientOpt = accountRepository.findByAccountNumber(request.getToAccountNumber());
        if (recipientOpt.isEmpty() || recipientOpt.get().getStatus() != AccountStatus.ACTIVE) {
            return new InterBankTransactionResponse(
                    false, "Račun primaoca ne postoji ili je neaktivan.",
                    null, null, null, null
            );
        }

        Account recipient = recipientOpt.get();

        if (!recipient.getCurrency().getCode().equals(request.getToCurrencyId())) {
            return new InterBankTransactionResponse(
                    false, "Valuta primaoca se ne poklapa sa očekivanom valutom.",
                    null, null, null, null
            );
        }

        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal finalAmount = request.getAmount();

        if (!request.getFromCurrencyId().equals(request.getToCurrencyId())) {
            try {
                String json = exchangeRateClient.getConversionPair(
                        request.getFromCurrencyId(),
                        request.getToCurrencyId()
                );
                JsonNode node = objectMapper.readTree(json);
                exchangeRate = new BigDecimal(node.get("conversion_rate").asText());

                BigDecimal grossConverted = request.getAmount().multiply(exchangeRate);
                fee = grossConverted.multiply(BigDecimal.valueOf(0.01)); // 1% provizija
                finalAmount = grossConverted.subtract(fee);
            } catch (Exception e) {
                return new InterBankTransactionResponse(
                        false, "Greska pri konverziji valuta: " + e.getMessage(),
                        null, null, null, null
                );
            }
        }

        return new InterBankTransactionResponse(
                true,
                "Spremni za commit",
                finalAmount,
                request.getToCurrencyId(),
                exchangeRate,
                fee
        );
    }

    @Transactional
    public void commit(InterBankTransactionRequest request) {
        Account recipient = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new RuntimeException("Racun primaoca ne postoji"));

        recipient.setBalance(recipient.getBalance().add(request.getAmount()));
        recipient.setAvailableBalance(recipient.getBalance());
        accountRepository.save(recipient);
    }

    @Transactional
    public void cancel(InterBankTransactionRequest request) {
        Account recipient = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElse(null);

        if (recipient == null) {
            System.err.println("Ne postoji primalac za rollback: " + request.getToAccountNumber());
            return;
        }

        BigDecimal currentBalance = recipient.getBalance();

        // Proveri da li je rollback moguc – mora imati dovoljno sredstava
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            System.err.println("Nedovoljno sredstava za rollback. Trenutni balance: " + currentBalance);
            return;
        }

        // Rollback
        recipient.setBalance(recipient.getBalance().subtract(request.getAmount()));
        recipient.setAvailableBalance(recipient.getBalance());
        accountRepository.save(recipient);

        System.out.println("Rollback uspesan: Sredstva oduzeta sa " + request.getToAccountNumber());
    }

}

package rs.raf.stock_service.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.TaxDto;
import rs.raf.stock_service.exceptions.AccountNotFoundException;
import rs.raf.stock_service.exceptions.InsufficientFundsException;

import java.math.BigDecimal;

@Component
public class BankClientFallbackFactory implements FallbackFactory<BankClient> {
    @Override
    public BankClient create(Throwable cause) {
        return new BankClient() {
            @Override
            public BigDecimal getAccountBalance(String accountNumber) {
                return null;
            }

            @Override
            public void handleTax(TaxDto taxDto) {

            }

            @Override
            public void updateAvailableBalance(String accountNumber, BigDecimal amount) {
                if (cause instanceof FeignException.BadRequest) {
                    throw new InsufficientFundsException(amount);
                }
                if (cause instanceof  FeignException.NotFound){
                    System.out.println("Caught FeignException.NotFound: " + cause);
                    throw new AccountNotFoundException(accountNumber);
                }
                throw new RuntimeException(cause);
            }

            @Override
            public void updateBalance(String accountNumber, BigDecimal amount) {
                if (cause instanceof FeignException.BadRequest) {
                    throw new InsufficientFundsException(amount);
                }
                if (cause instanceof  FeignException.NotFound){
                    throw new AccountNotFoundException(accountNumber);
                }
                throw new RuntimeException(cause);
            }
        };
    }
}
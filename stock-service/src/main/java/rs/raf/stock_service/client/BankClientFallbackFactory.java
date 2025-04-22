package rs.raf.stock_service.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.exceptions.AccountNotFoundException;
import rs.raf.stock_service.exceptions.InsufficientFundsException;

import java.math.BigDecimal;

@Component
public class BankClientFallbackFactory implements FallbackFactory<BankClient> {
    @Override
    public BankClient create(Throwable cause) {
        return new BankClient() {
            @Override
            public ResponseEntity<PaymentDto> createPayment(CreatePaymentDto dto) {
                return null;
            }

            @Override
            public ResponseEntity<String> getUSDAccountNumberByClientId(Long clientId) {
                return null;
            }

            @Override
            public void rejectPayment(Long paymentId) {

            }

            @Override
            public void confirmPayment(Long paymentId) {

            }

            @Override
            public ResponseEntity<PaymentDto> executeSystemPayment(ExecutePaymentDto dto) {
                return null;
            }

            @Override
            public BigDecimal getAccountBalance(String accountNumber) {
                return null;
            }

            @Override
            public void handleTax(ExecutePaymentDto executePaymentDto) {

            }

            @Override
            public BigDecimal convert(ConvertDto convertDto) {
                return null;
            }

            @Override
            public AccountDetailsDto getAccountDetails(String accountNumber) {
                return null;
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
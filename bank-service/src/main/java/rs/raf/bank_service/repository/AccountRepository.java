package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Currency;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    List<Account> findAllByClientId(@Param("clientId") Long clientId);

    List<Account> findAllByClientIdAndCurrency_Code(@Param("clientId") Long clientId, @Param("currency") String code);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndClientId(String accountNumber, Long clientId);

    List<Account> findByClientId(Long clientId);

    Optional<CompanyAccount> findFirstByCurrencyAndCompanyId(Currency currency, Long companyId);

    boolean existsByNameAndClientId(String name, Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM accounts a WHERE a.accountNumber = :accountNumber")
    Account findByIdForUpdate(@Param("accountNumber") String accountNumber);


}

package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Currency;

import java.util.List;
import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    List<Account> findAllByClientId(@Param("clientId")Long clientId);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByClientId(Long clientId);

    Optional<CompanyAccount> findFirstByCurrencyAndCompanyId(Currency currency, Long companyId);


    boolean existsByAccountNumberAndClientId(String accountNumber, Long clientId);


}

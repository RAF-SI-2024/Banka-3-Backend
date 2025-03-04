package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import rs.raf.bank_service.domain.entity.Account;

import java.util.List;
import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    List<Account> findAllByAccountNumber(String accountNumber);
    List<Account> findAllByClientId(@Param("clientId")Long clientId);
    Account findByAccountNumber(@Param("accountNumber") String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByClientId(Long clientId);
}

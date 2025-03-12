package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.bank_service.domain.entity.Account;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    List<Account> findAllByAccountNumber(String accountNumber);
    List<Account> findAllByClientId(@Param("clientId")Long clientId);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumberAndClientId(String accountNumber, Long clientId);
    List<Account> findByClientId(Long clientId);
    boolean existsByAccountNumberAndClientId(String accountNumber, Long clientId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM accounts a WHERE a.accountNumber = :accountNumber")
    Account findByIdForUpdate(@Param("accountNumber") String accountNumber);


}

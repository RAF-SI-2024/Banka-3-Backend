package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import rs.raf.bank_service.domain.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
=======
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.raf.bank_service.domain.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByClientId(Long clientId);
>>>>>>> upstream/main
}

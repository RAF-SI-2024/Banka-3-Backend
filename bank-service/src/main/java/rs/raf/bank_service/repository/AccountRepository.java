package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
}

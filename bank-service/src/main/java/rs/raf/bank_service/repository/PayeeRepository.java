package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.Payee;

import java.util.List;
import java.util.Optional;

public interface PayeeRepository extends JpaRepository<Payee, Long> {
    Optional<Payee> findByAccountNumber(String accountNumber);
    List<Payee> findByClientId(Long clientId); // Dodato za pretragu po clientId
}

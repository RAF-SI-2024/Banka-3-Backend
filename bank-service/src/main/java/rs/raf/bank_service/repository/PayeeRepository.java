package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Payee;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayeeRepository extends JpaRepository<Payee, Long> {

    Optional<Payee> findByAccountNumberandCliendId(String accountNumber, Long clientId);

    List<Payee> findByClientId(Long clientId);
}

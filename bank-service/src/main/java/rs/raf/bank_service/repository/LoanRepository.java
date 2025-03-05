package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Loan;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    // Možemo dodati custom metode ako su potrebne
}

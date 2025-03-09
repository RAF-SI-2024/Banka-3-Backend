package rs.raf.bank_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Installment;

import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    // Pronalazi sve rate vezane za određeni kredit
    List<Installment> findByLoanId(Long loanId);
}

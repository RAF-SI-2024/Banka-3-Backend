package rs.raf.bank_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.LoanStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {
    Page<Loan> findByAccountIn(Collection<Account> accounts, Pageable pageable);

    List<Loan> findByStatus(LoanStatus loanStatus);

    List<Loan> findByNextInstallmentDate(LocalDate localDate);

    List<Loan> findByNextInstallmentDateAndStartDateBefore(LocalDate nextInstallmentDate, LocalDate beforeStartDate);


}

package rs.raf.bank_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long>, JpaSpecificationExecutor<LoanRequest> {
    // Pronalazi zahteve po statusu
    List<LoanRequest> findByStatus(LoanRequestStatus status);
    Optional<LoanRequest> findByIdAndStatus(Long id, LoanRequestStatus status);
    Page<LoanRequest> findByAccountIn(List<Account> accounts, Pageable pageable);
}
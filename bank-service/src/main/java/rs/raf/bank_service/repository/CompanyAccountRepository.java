package rs.raf.bank_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.CompanyAccount;

public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, Long> {
    Page<CompanyAccount> findByCompanyId(Long companyId, Pageable pageable);
}

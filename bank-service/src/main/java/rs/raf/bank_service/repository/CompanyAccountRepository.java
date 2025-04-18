package rs.raf.bank_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.CompanyAccount;

import java.util.List;

public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, Long> {
    Page<CompanyAccount> findByCompanyId(Long companyId, Pageable pageable);

    List<CompanyAccount> findByCompanyIdAndCurrency_Code(Long companyId, String code);

    CompanyAccount findByCompanyId(Long companyId);
}


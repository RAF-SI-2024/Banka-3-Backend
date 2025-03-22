package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.OptionContract;

public interface OptionContractRepository extends JpaRepository<OptionContract, Long> {
    OptionContract findByOptionSymbol(String optionSymbol);
}

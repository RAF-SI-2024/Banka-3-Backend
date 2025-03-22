package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.FuturesContract;

public interface FuturesContractRepository extends JpaRepository<FuturesContract, Long> {
    FuturesContract findByFuturesSymbol(String futuresSymbol);
}

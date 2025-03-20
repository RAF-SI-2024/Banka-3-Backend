package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.CmeFuturesApiClient;
import rs.raf.bank_service.client.dto.CmeFuturesResponseDto;
import rs.raf.bank_service.domain.entity.FuturesContract;
import rs.raf.bank_service.repository.FuturesContractRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class FuturesService {

    private final FuturesContractRepository futuresContractRepository;
    private final CmeFuturesApiClient cmeFuturesApiClient;

    @Transactional
    public FuturesContract fetchAndSaveFutures(String symbol) {
        CmeFuturesResponseDto dto = cmeFuturesApiClient.fetchFuturesData(symbol);

        if (dto == null) {
            return null;
        }

        FuturesContract fc = new FuturesContract();
        fc.setFuturesSymbol(dto.getSymbol());
        fc.setUnderlyingProduct(dto.getUnderlying());
        LocalDate settlement = LocalDate.parse(dto.getSettlementDate());
        fc.setSettlementDate(settlement);

        fc.setContractSize(dto.getContractSize());
        fc.setContractUnit(dto.getUnit());

        if (dto.getPrice() != null) {
            BigDecimal price = BigDecimal.valueOf(dto.getPrice());
            fc.setPrice(price);
            BigDecimal mm = price.multiply(BigDecimal.valueOf(fc.getContractSize()))
                    .multiply(BigDecimal.valueOf(0.1));
            fc.setMaintenanceMargin(mm);
        }

        return futuresContractRepository.save(fc);
    }

    public FuturesContract getFuturesBySymbol(String symbol) {
        return futuresContractRepository.findByFuturesSymbol(symbol);
    }

    public List<FuturesContract> getAllFutures() {
        return futuresContractRepository.findAll();
    }
}

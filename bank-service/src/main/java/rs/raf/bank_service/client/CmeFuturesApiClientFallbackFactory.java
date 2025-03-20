package rs.raf.bank_service.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.client.dto.CmeFuturesResponseDto;

@Component
public class CmeFuturesApiClientFallbackFactory implements FallbackFactory<CmeFuturesApiClient> {

    @Override
    public CmeFuturesApiClient create(Throwable cause) {
        return new CmeFuturesApiClient() {
            @Override
            public CmeFuturesResponseDto fetchFuturesData(String symbol) {
                System.err.println("CmeFuturesApiClient fallback triggered: " + cause);

                CmeFuturesResponseDto dto = new CmeFuturesResponseDto();
                dto.setSymbol(symbol);
                dto.setUnderlying("Crude Oil (Dummy)");
                dto.setSettlementDate("2025-12-31");
                dto.setContractSize(5000);
                dto.setUnit("barrel");
                dto.setPrice(87.5);

                return dto;
            }
        };
    }
}

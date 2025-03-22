package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rs.raf.bank_service.client.dto.CmeFuturesResponseDto;

@FeignClient(
        name = "cmeFuturesApiClient",
        url = "${feign.url.cme}",
        fallbackFactory = CmeFuturesApiClientFallbackFactory.class
)
public interface CmeFuturesApiClient {

    @GetMapping("/some/futures/endpoint")
    CmeFuturesResponseDto fetchFuturesData(@RequestParam("symbol") String symbol);
}

package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import rs.raf.bank_service.client.dto.YahooOptionChainResponseDto;

/**
 * Yahoo Options API: https://query2.finance.yahoo.com/v7/finance/options/{ticker}
 */
@FeignClient(name = "yahooOptionApiClient", url = "https://query2.finance.yahoo.com/v7/finance")
public interface YahooOptionApiClient {

    @GetMapping("/options/{ticker}")
    YahooOptionChainResponseDto fetchOptions(@PathVariable("ticker") String ticker);
}

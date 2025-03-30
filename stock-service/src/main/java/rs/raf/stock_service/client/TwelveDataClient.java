package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rs.raf.stock_service.configuration.TwelveDataConfig;

@FeignClient(name = "twelvedata", url = "${twelvedata.base.url}", configuration = TwelveDataConfig.class)
public interface TwelveDataClient {

    // Endpoint za sve akcije
    @GetMapping("/stocks")
    String getAllStocks(@RequestParam("apikey") String dummy);

    // Endpoint za sve forex parove
    @GetMapping("/forex_pairs")
    String getAllForexPairs(@RequestParam("apikey") String dummy);

    // Time series za price history
    @GetMapping("/time_series")
    String getTimeSeries(
            @RequestParam("symbol") String symbol,
            @RequestParam("interval") String interval,
            @RequestParam(value = "outputsize", required = false, defaultValue = "30") String outputSize
    );
}

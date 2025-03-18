package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rs.raf.stock_service.configuration.AlphavantageConfig;

@FeignClient(name = "alphavantage", url = "${alphavantage.base.url}", configuration = AlphavantageConfig.class)
public interface AlphavantageClient {

    // Endpoint za GLOBAL_QUOTE
    @GetMapping("/query?function=GLOBAL_QUOTE")
    String getGlobalQuote(@RequestParam("symbol") String symbol);

    // Endpoint za Company Overview
    @GetMapping("/query?function=OVERVIEW")
    String getCompanyOverview(@RequestParam("symbol") String symbol);

    // Endpoint za search po ticker-u
    @GetMapping("/query?function=SYMBOL_SEARCH")
    String searchByTicker(@RequestParam("keywords") String keyword);

    // Endpoint za Forex Exchange Rate
    @GetMapping("/query?function=CURRENCY_EXCHANGE_RATE")
    String getCurrencyExchangeRate(@RequestParam("from_currency") String fromCurrency,
                                   @RequestParam("to_currency") String toCurrency);
}

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

    // Endpoint za Stock intraday
    @GetMapping("/query?function=TIME_SERIES_INTRADAY")
    String getIntradayData(@RequestParam("symbol") String symbol,
                           @RequestParam("interval") String interval,
                           @RequestParam(value = "outputsize", defaultValue = "compact") String outputsize,
                           @RequestParam(value = "datatype", defaultValue = "json") String datatype);

    // Endpoint za FX_INTRADAY
    @GetMapping("/query?function=FX_INTRADAY")
    String getForexPriceHistory(@RequestParam("from_symbol") String fromSymbol,
                                @RequestParam("to_symbol") String toSymbol,
                                @RequestParam("interval") String interval,
                                @RequestParam(value = "outputsize", required = false, defaultValue = "compact") String outputsize);

    //Endpoint za bulk insert stockova
    @GetMapping("/query?function=REALTIME_BULK_QUOTES")
    String getRealtimeBulkQuotes(@RequestParam("symbol") String symbols);


}

package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import rs.raf.stock_service.configuration.ExchangeRateApiConfig;

@FeignClient(name = "exchangerate", url = "${exchangerate.base.url}", configuration = ExchangeRateApiConfig.class)
public interface ExchangeRateApiClient {

    // Ruta za konverziju valuta, npr. /{apiKey}/pair/USD/EUR
    @GetMapping("/{apiKey}/pair/{base}/{target}")
    String getConversionPair(@PathVariable("apiKey") String dummy,
                             @PathVariable("base") String base,
                             @PathVariable("target") String target);

    // Ruta za dobijanje svih konverzionih stopa za dati base currency, npr. /{apiKey}/latest/USD
    @GetMapping("/{apiKey}/latest/{base}")
    String getLatestRates(@PathVariable("apiKey") String dummy,
                          @PathVariable("base") String base);
}

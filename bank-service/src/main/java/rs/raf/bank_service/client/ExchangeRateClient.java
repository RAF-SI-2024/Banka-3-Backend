package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import rs.raf.bank_service.domain.dto.UpdateExchangeRateDto;

@FeignClient(name = "exchangeRateClient", url = "https://v6.exchangerate-api.com/v6/4e7f3fa3d4807f67a453fad5")
public interface ExchangeRateClient {

    @GetMapping("/latest/{currencyCode}")
    UpdateExchangeRateDto getExchangeRates(@PathVariable("currencyCode") String currencyCode);

    // Ruta za konverziju valuta, npr. /{apiKey}/pair/USD/EUR
    @GetMapping("/pair/{base}/{target}")
    String getConversionPair(@PathVariable("base") String base,
                             @PathVariable("target") String target);
}

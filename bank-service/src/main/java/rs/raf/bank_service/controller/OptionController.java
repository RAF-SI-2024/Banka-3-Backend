package rs.raf.bank_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.entity.OptionContract;
import rs.raf.bank_service.service.OptionService;

import java.util.List;

@RestController
@RequestMapping("/api/options")
@AllArgsConstructor
public class OptionController {

    private final OptionService optionService;

    @GetMapping
    public ResponseEntity<List<OptionContract>> getAllOptions() {
        return ResponseEntity.ok(optionService.getAllOptions());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<OptionContract> getOption(@PathVariable String symbol) {
        OptionContract oc = optionService.getBySymbol(symbol);
        return oc != null
                ? ResponseEntity.ok(oc)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<OptionContract>> fetchOptions(@RequestParam String ticker) {
        List<OptionContract> saved = optionService.fetchOptionsFromYahoo(ticker);
        return ResponseEntity.ok(saved);
    }
}

package rs.raf.bank_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.entity.FuturesContract;
import rs.raf.bank_service.service.FuturesService;

import java.util.List;

@RestController
@RequestMapping("/api/futures")
@AllArgsConstructor
public class FuturesController {

    private final FuturesService futuresService;

    @GetMapping
    public ResponseEntity<List<FuturesContract>> getAllFutures() {
        return ResponseEntity.ok(futuresService.getAllFutures());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<FuturesContract> getFuturesBySymbol(@PathVariable String symbol) {
        FuturesContract fc = futuresService.getFuturesBySymbol(symbol);
        return fc != null
                ? ResponseEntity.ok(fc)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/fetch")
    public ResponseEntity<FuturesContract> fetchFutures(@RequestParam String symbol) {
        FuturesContract saved = futuresService.fetchAndSaveFutures(symbol);
        return ResponseEntity.ok(saved);
    }
}

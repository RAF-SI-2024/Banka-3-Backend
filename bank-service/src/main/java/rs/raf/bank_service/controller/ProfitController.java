package rs.raf.bank_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.bank_service.domain.dto.BankProfitResponseDto;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.service.PaymentService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/profit")
@RequiredArgsConstructor
public class ProfitController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<BankProfitResponseDto> getProfit() {
        BigDecimal exchangeProfit = paymentService.getExchangeProfit();

        return ResponseEntity.ok(new BankProfitResponseDto(exchangeProfit));
    }
}



package rs.raf.bank_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.enums.TransactionStatus;
import rs.raf.bank_service.service.PaymentReviewService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentReviewController {

    private final PaymentReviewService service;

    @GetMapping
    public ResponseEntity<List<PaymentOverviewDto>> getPayments(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) TransactionStatus paymentStatus
    ) {
        List<PaymentOverviewDto> payments = service.getPayments(token, startDate, endDate, minAmount, maxAmount, paymentStatus);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailsDto> getPaymentDetails(@PathVariable Long id) {
        PaymentDetailsDto details = service.getPaymentDetails(id);
        return ResponseEntity.ok(details);
    }
}

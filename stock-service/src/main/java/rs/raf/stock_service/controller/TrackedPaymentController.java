package rs.raf.stock_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.StockProfitResponseDto;
import rs.raf.stock_service.domain.dto.TrackedPaymentDto;
import rs.raf.stock_service.domain.dto.TrackedPaymentNotifyDto;
import rs.raf.stock_service.exceptions.TrackedPaymentNotFoundException;
import rs.raf.stock_service.service.OrderService;
import rs.raf.stock_service.service.TrackedPaymentNotificationService;
import rs.raf.stock_service.service.TrackedPaymentService;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tracked-payment")
@RequiredArgsConstructor
public class TrackedPaymentController {

    private final TrackedPaymentNotificationService trackedPaymentNotificationService;
    private final TrackedPaymentService trackedPaymentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<?> getTrackedPaymentStatus(@PathVariable Long id) {
        try {
            TrackedPaymentDto trackedPaymentDto = trackedPaymentService.getTrackedPaymentStatus(id);
            return ResponseEntity.ok(trackedPaymentDto);
        } catch (TrackedPaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }

    // ovo se ne koristi na frontu
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/success")
    public ResponseEntity<Void> handleExercisePaymentSuccessful(@RequestBody @Valid TrackedPaymentNotifyDto trackedPaymentNotifyDto) {
        trackedPaymentNotificationService.markAsSuccess(trackedPaymentNotifyDto.getCallbackId());
        return ResponseEntity.ok().build();
    }

    // ovo se ne koristi na frontu
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/fail")
    public ResponseEntity<Void> handleExercisePaymentFailed(@RequestBody @Valid TrackedPaymentNotifyDto trackedPaymentNotifyDto) {
        trackedPaymentNotificationService.markAsFail(trackedPaymentNotifyDto.getCallbackId());
        return ResponseEntity.ok().build();
    }
}



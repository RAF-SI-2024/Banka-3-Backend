package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.TransactionStatus;
import rs.raf.bank_service.domain.mapper.PaymentReviewMapper;
import rs.raf.bank_service.exceptions.PaymentNotFoundException;
import rs.raf.bank_service.repository.PaymentReviewRepository;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReviewService {

    private final PaymentReviewRepository repository;
    private final PaymentReviewMapper mapper;
    private final JwtTokenUtil jwtTokenUtil;

    // Dohvatanje svih transakcija za određenog klijenta sa filtriranjem
    public List<PaymentOverviewDto> getPayments(
            String token,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            TransactionStatus paymentStatus
    ) {
        Long clientId = getClientIdFromToken(token); // Dobijamo clientId iz tokena
        List<Payment> payments = repository.findByClientIdAndTransactionDateBetweenAndAmountBetweenAndPaymentStatus(
                clientId, startDate, endDate, minAmount, maxAmount, paymentStatus
        );
        return payments.stream()
                .map(mapper::toOverviewDto)
                .toList();
    }

    // Dohvatanje detalja transakcije po ID-u
    public PaymentDetailsDto getPaymentDetails(Long id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return mapper.toDetailsDto(payment);
    }

    // Metoda za dobijanje clientId iz tokena
    private Long getClientIdFromToken(String token) {
        token = token.replace("Bearer ", "");
        if (!jwtTokenUtil.validateToken(token)) {
            throw new SecurityException("Invalid token");
        }
        return Long.valueOf(jwtTokenUtil.extractUserId(token));
    }
}

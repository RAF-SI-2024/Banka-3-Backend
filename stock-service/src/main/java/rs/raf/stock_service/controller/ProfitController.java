package rs.raf.stock_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.ActuaryProfitDto;
import rs.raf.stock_service.domain.dto.StockProfitResponseDto;
import rs.raf.stock_service.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/profit")
@RequiredArgsConstructor
public class ProfitController {

    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<StockProfitResponseDto> getProfit() {
        BigDecimal stockCommissionProfit = orderRepository.getBankProfitFromOrders();
        List<ActuaryProfitDto> actuaryProfits = orderRepository.getActuaryProfits();
        return ResponseEntity.ok(new StockProfitResponseDto(stockCommissionProfit, actuaryProfits));
    }
}



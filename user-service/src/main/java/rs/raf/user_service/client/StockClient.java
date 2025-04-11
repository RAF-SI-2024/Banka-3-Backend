package rs.raf.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import rs.raf.user_service.domain.dto.OrderDto;

import java.util.List;

@FeignClient(name = "stock-service", url = "${spring.cloud.openfeign.client.config.stock-service.url}")
public interface StockClient {
    @GetMapping("api/orders/all")
    List<OrderDto> getAll();
}

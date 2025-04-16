package pack.userservicekotlin.external

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import pack.userservicekotlin.domain.dto.external.OrderDto

@FeignClient(
    name = "stock-service",
    url = "\${spring.cloud.openfeign.client.config.stock-service.url}",
)
interface StockClient {
    @GetMapping("api/orders/all")
    fun getAll(): List<OrderDto>
}

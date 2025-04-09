package pack.userservicekotlin.external

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping

@FeignClient(
    name = "bank-service",
    url = "\${spring.cloud.openfeign.client.config.bank-service.url}",
    fallbackFactory = BankClientFallbackFactory::class,
)
interface BankClient {
    @PostMapping("/api/payment/confirm-payment/{id}")
    fun confirmPayment(
        @PathVariable("id") id: Long?,
    )

    @PostMapping("/api/payment/confirm-transfer/{id}")
    fun confirmTransfer(
        @PathVariable("id") id: Long?,
    )

    @PutMapping("/api/account/{id}/change-limit")
    fun changeAccountLimit(
        @PathVariable("id") id: Long?,
    )

    @PutMapping("/api/account/1/cards/approve/{id}")
    fun approveCardRequest(
        @PathVariable("id") id: Long?,
    )

    @PostMapping("/api/payment/reject-payment/{id}")
    fun rejectConfirmPayment(
        @PathVariable("id") id: Long?,
    )

    @PostMapping("/api/payment/reject-transfer/{id}")
    fun rejectConfirmTransfer(
        @PathVariable("id") id: Long?,
    ) //

    @PutMapping("/api/account/{id}/change-limit/reject")
    fun rejectChangeAccountLimit(
        @PathVariable("id") id: Long?,
    ) //

    @PutMapping("/api/account/1/cards/reject/{id}")
    fun rejectApproveCardRequest(
        @PathVariable("id") id: Long?,
    ) //
}

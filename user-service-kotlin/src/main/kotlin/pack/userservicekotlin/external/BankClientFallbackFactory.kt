package pack.userservicekotlin.external

import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component

@Component
class BankClientFallbackFactory : FallbackFactory<BankClient> {
    override fun create(cause: Throwable): BankClient =
        object : BankClient {
            override fun confirmPayment(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun confirmTransfer(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun changeAccountLimit(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun approveCardRequest(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun rejectConfirmPayment(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun rejectConfirmTransfer(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun rejectChangeAccountLimit(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")

            override fun rejectApproveCardRequest(id: Long?): Unit = throw RuntimeException("Unable to communicate with Bank Service")
        }
}

package pack.userservicekotlin.controller

import feign.FeignException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pack.userservicekotlin.domain.dto.verification.CreateVerificationRequestDto
import pack.userservicekotlin.service.ClientService
import pack.userservicekotlin.service.VerificationRequestService
import pack.userservicekotlin.swagger.VerificationRequestApiDoc

@RestController
@RequestMapping("/api/verification")
class VerificationRequestController(
    private val verificationRequestService: VerificationRequestService,
    private val clientService: ClientService,
) : VerificationRequestApiDoc {
    @PreAuthorize("hasRole('CLIENT')")
    override fun getActiveRequests(userAgent: String?): ResponseEntity<*> {
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        }

        return clientService.getCurrentClient().fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found: $it") },
            ifRight = { client ->
                verificationRequestService.getActiveRequests(client.id!!).fold(
                    ifLeft = { ResponseEntity.internalServerError().body(it.toString()) },
                    ifRight = { ResponseEntity.ok(it) },
                )
            },
        )
    }

    @PreAuthorize("hasRole('CLIENT')")
    override fun getRequestHistory(userAgent: String?): ResponseEntity<*> {
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        }

        return clientService.getCurrentClient().fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found: $it") },
            ifRight = { client ->
                verificationRequestService.getRequestHistory(client.id!!).fold(
                    ifLeft = { ResponseEntity.internalServerError().body(it.toString()) },
                    ifRight = { ResponseEntity.ok(it) },
                )
            },
        )
    }

    @PreAuthorize("hasRole('CLIENT')")
    override fun denyRequest(
        userAgent: String?,
        requestId: Long,
        authHeader: String,
    ): ResponseEntity<*> {
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        }

        return verificationRequestService.denyVerificationRequest(requestId, authHeader).fold(
            { ResponseEntity.badRequest().body(it.toString()) },
            { ResponseEntity.ok("Request denied successfully.") },
        )
    }

    @PreAuthorize("hasRole('ADMIN')")
    override fun createVerificationRequest(dto: CreateVerificationRequestDto): ResponseEntity<*> =
        verificationRequestService.createVerificationRequest(dto).fold(
            { ResponseEntity.internalServerError().body(it.toString()) },
            { ResponseEntity.ok("Verification request created.") },
        )

    @PreAuthorize("hasRole('CLIENT')")
    override fun approveRequest(
        userAgent: String?,
        requestId: Long,
        authHeader: String,
    ): ResponseEntity<*> {
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        }

        return try {
            verificationRequestService.processApproval(requestId, authHeader).fold(
                { ResponseEntity.badRequest().body(it.toString()) },
                { ResponseEntity.ok("Request approved.") },
            )
        } catch (e: FeignException.BadRequest) {
            ResponseEntity.badRequest().body("Bank service error: ${e.message}")
        }
    }
}

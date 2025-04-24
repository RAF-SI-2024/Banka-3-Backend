package pack.userservicekotlin.arrow

sealed class VerificationServiceError {
    data class RequestNotFound(
        val requestId: Long,
    ) : VerificationServiceError()

    data class InvalidRequestStatus(
        val message: String,
    ) : VerificationServiceError()

    data class DatabaseError(
        val message: String,
    ) : VerificationServiceError()

    data class BankServiceError(
        val message: String,
    ) : VerificationServiceError()
}

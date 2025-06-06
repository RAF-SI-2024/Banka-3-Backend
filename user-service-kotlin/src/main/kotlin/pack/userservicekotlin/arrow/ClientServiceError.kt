package pack.userservicekotlin.arrow

sealed class ClientServiceError {
    data class NotFound(
        val id: Long,
    ) : ClientServiceError()

    data class EmailNotFound(
        val email: String,
    ) : ClientServiceError()

    data object EmailAlreadyExists : ClientServiceError()

    data object JmbgAlreadyExists

     : ClientServiceError()

    data object UsernameAlreadyExists

     : ClientServiceError()

    data class RoleNotFound(
        val role: String,
    ) : ClientServiceError()

    data class NotAuthenticated(
        val user: String,
    ) : ClientServiceError()

    data object InvalidInput : ClientServiceError()

    data object Unknown : ClientServiceError()
}

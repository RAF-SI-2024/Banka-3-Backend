package pack.userservicekotlin.arrow

sealed class AuthorizedPersonnelServiceError {
    data class CompanyNotFound(
        val companyId: Long,
    ) : AuthorizedPersonnelServiceError()

    data class AuthorizedPersonnelNotFound(
        val id: Long,
    ) : AuthorizedPersonnelServiceError()
}

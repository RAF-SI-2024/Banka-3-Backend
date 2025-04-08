package pack.userservicekotlin.arrow

sealed class EmployeeServiceError {
    data object NotFound : EmployeeServiceError()

    data object RoleNotFound : EmployeeServiceError()

    data object UserConflict : EmployeeServiceError()

    data object LimitNotFound : EmployeeServiceError()

    data object EmailAlreadyExists : EmployeeServiceError()

    data class Unknown(
        val cause: Throwable,
    ) : EmployeeServiceError()
}

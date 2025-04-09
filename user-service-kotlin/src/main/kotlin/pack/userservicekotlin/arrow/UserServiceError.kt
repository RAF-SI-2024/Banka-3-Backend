package pack.userservicekotlin.arrow

sealed class UserServiceError {
    data object UserNotFound : UserServiceError()

    data object RoleNotFound : UserServiceError()

    data object RoleAlreadyAssigned : UserServiceError()

    data object RoleNotAssigned : UserServiceError()
}

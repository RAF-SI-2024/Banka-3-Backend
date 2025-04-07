package pack.userservicekotlin.arrow

sealed class AuthServiceError {
    data object InvalidCredentials : AuthServiceError()

    data object UserNotFound : AuthServiceError()

    data object InvalidToken : AuthServiceError()

    data object ExpiredToken : AuthServiceError()
}

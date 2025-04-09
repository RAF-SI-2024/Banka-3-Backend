package pack.userservicekotlin.arrow

sealed class ActuaryServiceError {
    data class EmployeeNotFound(
        val id: Long,
    ) : ActuaryServiceError()

    data class ActuaryLimitNotFound(
        val employeeId: Long,
    ) : ActuaryServiceError()

    data class NotAnAgent(
        val employeeId: Long,
    ) : ActuaryServiceError()
}

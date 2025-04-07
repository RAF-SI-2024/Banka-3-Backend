package pack.userservicekotlin.arrow

sealed class CompanyServiceError {
    data class OwnerNotFound(
        val ownerId: Long,
    ) : CompanyServiceError()

    data class ActivityCodeNotFound(
        val activityCodeId: String,
    ) : CompanyServiceError()

    data class RegistrationNumberExists(
        val regNum: String,
    ) : CompanyServiceError()

    data class TaxIdExists(
        val taxId: String,
    ) : CompanyServiceError()

    data class CompanyNotFound(
        val companyId: Long,
    ) : CompanyServiceError()
}

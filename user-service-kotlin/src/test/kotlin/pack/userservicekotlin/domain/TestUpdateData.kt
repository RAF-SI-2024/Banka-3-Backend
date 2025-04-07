package pack.userservicekotlin.domain

import pack.userservicekotlin.domain.dto.client.UpdateClientDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto

object TestUpdateData {
    fun validUpdateEmployeeDto() =
        UpdateEmployeeDto(
            lastName = "Doe",
            gender = "M",
            phone = "0612345678",
            address = "Main Street 1",
            position = "Senior Dev",
            department = "Engineering",
            role = "EMPLOYEE",
        )

    fun validUpdateClientDto() =
        UpdateClientDto(
            lastName = "Doe",
            gender = "F",
            phone = "0612345678",
            address = "Client Street 2",
        )
}

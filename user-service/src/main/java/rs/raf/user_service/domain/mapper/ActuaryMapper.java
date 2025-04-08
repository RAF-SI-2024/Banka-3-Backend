package rs.raf.user_service.domain.mapper;

import rs.raf.user_service.domain.dto.ActuaryDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.entity.Employee;

import java.math.BigDecimal;

public class ActuaryMapper {
    public static ActuaryDto toDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        ActuaryDto dto = new ActuaryDto();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setPosition(employee.getPosition());
        dto.setProfit(BigDecimal.ZERO);
        return dto;
    }
}

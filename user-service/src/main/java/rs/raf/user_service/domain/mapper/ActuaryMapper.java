package rs.raf.user_service.domain.mapper;

import rs.raf.user_service.domain.dto.ActuaryDto;
import rs.raf.user_service.domain.dto.AgentDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.entity.Employee;

import java.math.BigDecimal;

public class ActuaryMapper {
    public static ActuaryDto toActuaryDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        ActuaryDto dto = new ActuaryDto();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setRole(employee.getRole().getName());
        dto.setProfit(BigDecimal.ZERO);
        return dto;
    }
    public static AgentDto toAgentDto(EmployeeDto employee,BigDecimal limitAmount, BigDecimal usedLimit, boolean needsApproval){
        if(employee == null)
            return null;
        AgentDto dto = new AgentDto();
        dto.setId(employee.getId());
        dto.setEmail(employee.getEmail());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setAddress(employee.getAddress());
        dto.setPhone(employee.getPhone());
        dto.setGender(employee.getGender());
        dto.setBirthDate(employee.getBirthDate());
        dto.setPosition(employee.getPosition());
        dto.setDepartment(employee.getDepartment());
        dto.setActive(employee.isActive());
        dto.setUsername(employee.getUsername());
        dto.setJmbg(employee.getJmbg());
        dto.setRole(employee.getRole());

        dto.setLimitAmount(limitAmount);
        dto.setUsedLimit(usedLimit);
        dto.setNeedsApproval(needsApproval);

        return dto;
    }

}

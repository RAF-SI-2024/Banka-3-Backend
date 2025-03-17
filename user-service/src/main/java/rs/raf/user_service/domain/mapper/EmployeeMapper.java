package rs.raf.user_service.domain.mapper;

import rs.raf.user_service.domain.dto.CreateEmployeeDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.entity.Employee;

public class EmployeeMapper {
    public static EmployeeDto toDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        EmployeeDto dto = new EmployeeDto();
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
        if (employee.getRole() != null) {
            dto.setRole(employee.getRole().getName());
        }


        return dto;
    }

    public static Employee createDtoToEntity(CreateEmployeeDto dto) {
        if (dto == null) {
            return null;
        }
        return new Employee(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getBirthDate(),
                dto.getGender(),
                dto.getEmail(),
                dto.getPhone(),
                dto.getAddress(),
                dto.getUsername(),
                dto.getPosition(),
                dto.getDepartment(),
                dto.getActive(),
                dto.getJmbg(),
                null // @todo maybe set role here
        );
    }
}

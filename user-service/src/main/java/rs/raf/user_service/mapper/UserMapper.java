package rs.raf.user_service.mapper;

import org.springframework.stereotype.Component;
import rs.raf.user_service.dto.UserDTO;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Employee;

@Component
public class UserMapper {

    public UserDTO toDto(BaseUser user) {
        if (user instanceof Employee employee) {
            return new UserDTO(
                    employee.getId(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmail(),
                    employee.getUsername(),
                    employee.isActive(),
                    employee.getPosition(),
                    employee.getDepartment()
            );
        } else {
            // Client nema dodatna polja
            return new UserDTO(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    //  Mapiranje iz UserDTO u Employee (pošto Client nema dodatna polja)
    public Employee toEntity(UserDTO dto) {
        return Employee.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .username(dto.getUsername())
                .active(dto.getActive())
                .position(dto.getPosition())
                .department(dto.getDepartment())
                .build();
    }
}

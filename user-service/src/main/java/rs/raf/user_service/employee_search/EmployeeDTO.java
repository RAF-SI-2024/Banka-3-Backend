package rs.raf.user_service.employee_search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeDTO {
    private Long id;
    private String username;
    private String position;
    private String department;
    private boolean active;

    public EmployeeDTO(String username, String position, String department, boolean active) {

        this.username = username;
        this.position = position;
        this.department = department;
        this.active = active;
    }


}

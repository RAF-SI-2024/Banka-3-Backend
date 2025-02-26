package rs.raf.user_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Entity
@DiscriminatorValue("EMP")
@Getter
@Setter
@SuperBuilder()
@RequiredArgsConstructor
@AllArgsConstructor
public class Employee extends BaseUser {

    @Column(updatable = false)
    private String username;

    private String position;

    private String department;

    private boolean active;

    public Employee(String firstName, String lastName, Date birthDate, String gender, String email, String phone,
                    String address, String username, String position, String department, Boolean active) {
        super(firstName, lastName, birthDate, gender, email, phone, address);
        this.username = username;
        this.position = position;
        this.department = department;
        this.active = active;
    }
}

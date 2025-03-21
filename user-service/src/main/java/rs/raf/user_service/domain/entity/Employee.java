package rs.raf.user_service.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.util.Date;


@Entity
@DiscriminatorValue("EMP")
@Getter
@Setter

@SuperBuilder()
@RequiredArgsConstructor
@AllArgsConstructor

public class Employee extends BaseUser {

    private String position;

    private String department;

    private boolean active;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.REMOVE)
    private ActuaryLimit actuaryLimit;

    public Employee(String firstName, String lastName, Date birthDate, String gender, String email, String phone,
                    String address, String username, String position, String department, Boolean active,
                    String jmbg, Role role) {
        super(firstName, lastName, birthDate, gender, email, phone, address, jmbg,username, role);
        this.position = position;
        this.department = department;
        this.active = active;
    }
}

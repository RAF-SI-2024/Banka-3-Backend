package rs.raf.user_service.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EMP")
@Getter
@Setter
public class Employee extends BaseUser {

    @Column(updatable = false)
    private String username;

    private String position;

    private String department;

    private boolean active;
}

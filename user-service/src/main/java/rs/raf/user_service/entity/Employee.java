package rs.raf.user_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
}

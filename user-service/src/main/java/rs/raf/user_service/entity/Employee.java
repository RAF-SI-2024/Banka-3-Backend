package rs.raf.user_service.entity;

import lombok.*;
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
}

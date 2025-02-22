package rs.raf.user_service.entity;

import javax.persistence.*;
import java.util.Date;

@Entity(name="users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class BaseUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(updatable = false)
    private String firstName;

    private String lastName;

    @Column(updatable = false)
    private Date birthDate;

    private String gender;

    @Column(updatable = false)
    private String email;

    private String phone;

    private String address;

    private String password;

}
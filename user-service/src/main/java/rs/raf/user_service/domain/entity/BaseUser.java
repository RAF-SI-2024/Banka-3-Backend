package rs.raf.user_service.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter

@SuperBuilder
@RequiredArgsConstructor
@AllArgsConstructor

public abstract class BaseUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(updatable = false, unique = true)
    private String username;

    @Column(updatable = false)
    private String firstName;

    private String lastName;

    @Column(updatable = false)
    private Date birthDate;

    private String gender;

    @Column(updatable = false, unique = true)
    private String email;

    private String phone;

    private String address;

    private String password;

    @Column(updatable = false, unique = true)
    private String jmbg;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    public BaseUser(String firstName, String lastName, Date birthDate, String gender, String email, String phone,
                    String address, String jmbg, String username, Role role) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.jmbg = jmbg;
        this.role = role;
    }
}

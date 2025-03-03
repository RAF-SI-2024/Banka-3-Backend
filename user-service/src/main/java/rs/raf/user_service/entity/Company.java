package rs.raf.user_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Entity
@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(updatable = false, unique = true)
    private String registrationNumber;

    @Column(updatable = false, unique = true)
    private String taxId;

    private String activityCode;

    private String address;

    @ManyToOne
    private Client majorityOwner;
}
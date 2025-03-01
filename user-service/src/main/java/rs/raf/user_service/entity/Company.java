package rs.raf.user_service.entity;

import javax.persistence.*;

@Entity
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true)
    private String registrationNumber;
    @Column(unique = true)
    private String taxId;
    private Integer activityCode; // sifra placanja
    private String address;
    @ManyToOne
    private Client majorityOwner;
}
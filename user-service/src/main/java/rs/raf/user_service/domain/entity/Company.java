package rs.raf.user_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import lombok.Builder;


import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuthorizedPersonel> authorizedPersonel = new ArrayList<>();
}
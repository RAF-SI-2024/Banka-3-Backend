package rs.raf.user_service.domain.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CLT")
@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
public class Client extends BaseUser {
    @OneToMany(mappedBy = "majorityOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Company> companies = new ArrayList<>();
}

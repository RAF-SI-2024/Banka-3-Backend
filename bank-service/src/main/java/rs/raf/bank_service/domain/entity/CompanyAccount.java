package rs.raf.bank_service.domain.entity;

<<<<<<< HEAD
import lombok.*;
import lombok.experimental.SuperBuilder;
=======
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
>>>>>>> upstream/main

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("COMP")
@Getter
@Setter
@RequiredArgsConstructor
public class CompanyAccount extends Account {
    private Long companyId;

}

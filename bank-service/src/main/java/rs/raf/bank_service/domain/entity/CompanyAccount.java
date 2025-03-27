package rs.raf.bank_service.domain.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("COMP")
@Getter
@Setter
@RequiredArgsConstructor
@SuperBuilder
public class CompanyAccount extends Account {
    private Long companyId;
    private Long authorizedPersonId;

}

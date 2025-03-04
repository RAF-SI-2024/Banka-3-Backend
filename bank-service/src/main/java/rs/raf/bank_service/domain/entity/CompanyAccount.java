package rs.raf.bank_service.domain.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


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

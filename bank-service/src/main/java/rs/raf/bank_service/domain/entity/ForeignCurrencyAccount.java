package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.ForeignAccountType;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Getter
@Setter
@Entity
@DiscriminatorValue("FOREIGN")
public class ForeignCurrencyAccount extends Account {
    @Enumerated(EnumType.STRING)
    private ForeignAccountType subType; // podvrsta
}
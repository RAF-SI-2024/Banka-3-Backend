package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("PER")
@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
public class PersonalAccount extends Account {


}

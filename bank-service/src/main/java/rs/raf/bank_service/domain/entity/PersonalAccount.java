package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
<<<<<<< HEAD
=======

>>>>>>> upstream/main
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("PER")
@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
<<<<<<< HEAD
public class PersonalAccount extends Account{
=======
public class PersonalAccount extends Account {
>>>>>>> upstream/main

}

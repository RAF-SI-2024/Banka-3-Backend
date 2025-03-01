package rs.raf.user_service.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CLT")
@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
public class Client extends BaseUser {

}

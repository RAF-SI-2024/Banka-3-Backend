package rs.raf.user_service.entity;

import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CLT")
@SuperBuilder
@RequiredArgsConstructor
public class Client extends BaseUser {
}

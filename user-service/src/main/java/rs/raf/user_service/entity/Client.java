package rs.raf.user_service.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CLT")
public class Client extends BaseUser {
}

package rs.raf.user_service.entity;

import javax.persistence.*;

@Entity
@DiscriminatorValue("CLT")
public class Client extends BaseUser {
}

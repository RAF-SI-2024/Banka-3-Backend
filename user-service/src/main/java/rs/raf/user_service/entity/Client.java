package rs.raf.user_service.entity;

import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import javax.persistence.*;

@Entity
@DiscriminatorValue("CLT")
@SuperBuilder
@RequiredArgsConstructor
public class Client extends BaseUser {
}

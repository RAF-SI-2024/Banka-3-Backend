package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.VerificationStatus;

import javax.persistence.*;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "change_limit_request")
public class ChangeLimitRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;

    @Positive(message = "Limit must be greater than zero")
    private BigDecimal newLimit;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // PENDING, APPROVED, DENIED

    public ChangeLimitRequest() {
    }

    public ChangeLimitRequest(String accountNumber, BigDecimal newLimit) {
        this.accountNumber = accountNumber;
        this.newLimit = newLimit;
        this.status = VerificationStatus.PENDING;
    }


}

package rs.raf.user_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity(name = "actuary_limits")
@AllArgsConstructor
@RequiredArgsConstructor
public class ActuaryLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal limitAmount;
    private BigDecimal usedLimit;
    private boolean needsApproval;
    @OneToOne
    private Employee employee;

    public ActuaryLimit(BigDecimal limitAmount, BigDecimal usedLimit, boolean needsApproval, Employee employee) {
        this.limitAmount = limitAmount;
        this.usedLimit = usedLimit;
        this.needsApproval = needsApproval;
        this.employee = employee;
    }
}

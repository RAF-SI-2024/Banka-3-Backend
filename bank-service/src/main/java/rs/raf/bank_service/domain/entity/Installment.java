package rs.raf.bank_service.domain.entity;

import lombok.*;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.domain.enums.PaymentStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    private BigDecimal amount;
    private BigDecimal interestRate;
    private LocalDate expectedDueDate;
    private LocalDate actualDueDate;

    @Enumerated(EnumType.STRING)
    private InstallmentStatus installmentStatus;
}
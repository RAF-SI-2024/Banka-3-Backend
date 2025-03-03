package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "payees")
public class Payee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false) // ✅ Klijent ID je obavezan
    private Long clientId;
}


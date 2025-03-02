//package rs.raf.bank_service.domain.entity;
//
//import lombok.Getter;
//import lombok.Setter;
//import rs.raf.bank_service.domain.enums.AccountOwnerType;
//import javax.persistence.DiscriminatorValue;
//import javax.persistence.Entity;
//import javax.persistence.EnumType;
//import javax.persistence.Enumerated;
//import java.math.BigDecimal;
//
//@Getter
//@Setter
//@Entity
//@DiscriminatorValue("CURRENT")
//public class CurrentAccount extends Account {
//    @Enumerated(EnumType.STRING)
//    private AccountOwnerType subType; // podvrsta
//    private BigDecimal maintenanceFee;
//}
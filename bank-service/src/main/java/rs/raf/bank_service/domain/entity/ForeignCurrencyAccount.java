//package rs.raf.bank_service.domain.entity;
//
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import lombok.experimental.SuperBuilder;
//import rs.raf.bank_service.domain.enums.AccountType;
//import javax.persistence.DiscriminatorValue;
//import javax.persistence.Entity;
//import javax.persistence.EnumType;
//import javax.persistence.Enumerated;
//
//@Entity
//@DiscriminatorValue("CLT")
//@Getter
//@Setter
//@SuperBuilder
//@RequiredArgsConstructor
//public class ForeignCurrencyAccount extends Account {
//    @Enumerated(EnumType.STRING)
//    private AccountType subType; // podvrsta
//}
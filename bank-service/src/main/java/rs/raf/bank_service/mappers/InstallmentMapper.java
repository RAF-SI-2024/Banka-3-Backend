package rs.raf.bank_service.mappers;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.InstallmentCreateDto;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.entity.Installment;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InstallmentMapper {
    public InstallmentDto toDto(Installment installment) {
        return InstallmentDto.builder()
                .amount(installment.getAmount())
                .interestRate(installment.getInterestRate())
                .expectedDueDate(installment.getExpectedDueDate())
                .actualDueDate(installment.getActualDueDate())
                .paymentStatus(installment.getPaymentStatus())
                .build();
    }

    public List<InstallmentDto> toDtoList(List<Installment> installments) {
        return installments.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Installment toEntity(InstallmentCreateDto installmentDto) {
        Installment installment = new Installment();
        installment.setAmount(installmentDto.getAmount());
        installment.setInterestRate(installmentDto.getInterestRate());
        installment.setExpectedDueDate(installmentDto.getExpectedDueDate());
        installment.setActualDueDate(installmentDto.getActualDueDate());
        installment.setPaymentStatus(installmentDto.getPaymentStatus());
        return installment;
    }
}
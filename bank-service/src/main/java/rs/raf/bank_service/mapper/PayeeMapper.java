package rs.raf.bank_service.mapper;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.domain.entity.Payee;

@Component
public class PayeeMapper {

    public PayeeDto toDto(Payee payee) {
        PayeeDto dto = new PayeeDto();
        dto.setId(payee.getId());
        dto.setName(payee.getName());
        dto.setAccountNumber(payee.getAccountNumber());
        return dto;
    }

    public Payee toEntity(PayeeDto dto) {
        Payee payee = new Payee();
        payee.setId(dto.getId());
        payee.setName(dto.getName());
        payee.setAccountNumber(dto.getAccountNumber());
        return payee;
    }
}

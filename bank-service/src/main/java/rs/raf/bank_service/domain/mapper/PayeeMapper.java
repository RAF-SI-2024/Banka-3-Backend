package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.domain.entity.Payee;
import org.springframework.stereotype.Component;

@Component
public class PayeeMapper {

    public PayeeDto toDto(Payee payee) {
        if (payee == null) {
            return null;
        }

        PayeeDto payeeDto = new PayeeDto();
        payeeDto.setId(payee.getId());
        payeeDto.setName(payee.getName());
        payeeDto.setAccountNumber(payee.getAccountNumber());

        return payeeDto;
    }

    public Payee toEntity(PayeeDto payeeDto) {
        if (payeeDto == null) {
            return null;
        }

        Payee payee = new Payee();
        payee.setId(payeeDto.getId());
        payee.setName(payeeDto.getName());
        payee.setAccountNumber(payeeDto.getAccountNumber());

        return payee;
    }
}


package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.AccountTypeDto;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.enums.CurrentAccountType;
import rs.raf.bank_service.domain.enums.ForeignAccountType;
import rs.raf.bank_service.mapper.AccountMapper;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;

@Service
@AllArgsConstructor
public class CardService {

    CardRepository cardRepository;
    AccountMapper accountMapper;
    AccountRepository accountRepository;

    private boolean isBusiness(AccountTypeDto accountTypeDto){
        return !accountTypeDto.getSubtype().equals(CurrentAccountType.STANDARD.name()) &&
                !accountTypeDto.getSubtype().equals(CurrentAccountType.SAVINGS.name()) &&
                !accountTypeDto.getSubtype().equals(CurrentAccountType.RETIREMENT.name()) &&
                !accountTypeDto.getSubtype().equals(CurrentAccountType.STUDENT.name()) &&
                !accountTypeDto.getSubtype().equals(CurrentAccountType.UNEMPLOYED.name()) &&
                !accountTypeDto.getSubtype().equals(ForeignAccountType.PERSONAL.name());
    }

    public CardDto createCard(CreateCardDto createCardDto){
        return new CardDto();
    }


}

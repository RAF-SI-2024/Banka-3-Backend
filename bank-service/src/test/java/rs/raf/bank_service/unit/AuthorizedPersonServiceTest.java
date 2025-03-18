package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.AuthorizedPersonelDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.CompanyDto;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.InvalidAuthorizedPersonException;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.service.AccountService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthorizedPersonServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void setAuthorizedPerson_Success() {

        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setCompanyId(companyAccountId);
        companyAccount.setCompanyId(5L);
        companyAccount.setAccountNumber("123456789");

        CompanyDto companyDto = new CompanyDto();
        companyDto.setMajorityOwner(new ClientDto(employeeId, "Marko", "Markovic"));

        List<AuthorizedPersonelDto> personnelList = List.of(new AuthorizedPersonelDto(authorizedPersonId, "Petar", "Petrovic", 5L));


        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.of(companyAccount));
        when(userClient.getCompanyById(5L)).thenReturn(companyDto);
        when(userClient.getAuthorizedPersonnelByCompany(5L)).thenReturn(personnelList);


        accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);


        assertEquals(authorizedPersonId, companyAccount.getAuthorizedPersonId());
        verify(accountRepository).save(companyAccount);

    }

    @Test
    void setAuthorizedPerson_AccountNotFound() {

        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.empty());


        assertThrows(AccountNotFoundException.class, () -> {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
        });
    }

    @Test
    void setAuthorizedPerson_UnauthorizedUser() {

        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setCompanyId(companyAccountId);
        companyAccount.setCompanyId(5L);
        companyAccount.setAccountNumber("123456789");

        CompanyDto companyDto = new CompanyDto();
        companyDto.setMajorityOwner(new ClientDto(99L, "Dragan", "Jovanovic"));  // 👈 Employee nije vlasnik!

        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.of(companyAccount));
        when(userClient.getCompanyById(5L)).thenReturn(companyDto);


        assertThrows(UnauthorizedException.class, () -> {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
        });
    }

    @Test
    void setAuthorizedPerson_InvalidAuthorizedPerson() {

        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setCompanyId(companyAccountId);
        companyAccount.setCompanyId(5L);
        companyAccount.setAccountNumber("123456789");

        CompanyDto companyDto = new CompanyDto();
        companyDto.setMajorityOwner(new ClientDto(employeeId, "Marko", "Markovic"));

        List<AuthorizedPersonelDto> personnelList = List.of();

        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.of(companyAccount));
        when(userClient.getCompanyById(5L)).thenReturn(companyDto);
        when(userClient.getAuthorizedPersonnelByCompany(5L)).thenReturn(personnelList);


        assertThrows(InvalidAuthorizedPersonException.class, () -> {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
        });
    }
}

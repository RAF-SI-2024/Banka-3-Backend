package rs.raf.bank_service.service;

import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.AccountStatus;
import rs.raf.bank_service.domain.enums.AccountType;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.specification.AccountSearchSpecification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AccountService {
        private final CurrencyRepository currencyRepository;
        private final AccountRepository accountRepository;
        @Autowired
        private final UserClient userClient;

        @Operation(summary = "Retrieve accounts with filtering and pagination", description = "Returns a paginated list of accounts filtered by account number and owner's first and last name.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "No accounts found matching the criteria")
        })
        public Page<AccountDto> getAccounts(
                        @Parameter(description = "Filter accounts by part of the account number", example = "111111111111111111") String accountNumber,
                        @Parameter(description = "Filter accounts by owner's first name", example = "Marko") String firstName,
                        @Parameter(description = "Filter accounts by owner's last name", example = "Markovic") String lastName,
                        Pageable pageable) {

                Specification<Account> spec = Specification
                                .where(AccountSearchSpecification.accountNumberContains(accountNumber));
                List<Account> accounts = accountRepository.findAll(spec);

                List<AccountDto> accountDtos = accounts.stream().map(account -> {
                        ClientDto client = userClient.getClientById(account.getClientId());
                        return AccountMapper.toDto(account, client);
                }).collect(Collectors.toList());

                if (firstName != null && !firstName.isEmpty()) {
                        accountDtos = accountDtos.stream()
                                        .filter(dto -> dto.getOwner() != null &&
                                                        dto.getOwner().getFirstName() != null &&
                                                        dto.getOwner().getFirstName().toLowerCase()
                                                                        .contains(firstName.toLowerCase()))
                                        .collect(Collectors.toList());
                }

                if (lastName != null && !lastName.isEmpty()) {
                        accountDtos = accountDtos.stream()
                                        .filter(dto -> dto.getOwner() != null &&
                                                        dto.getOwner().getLastName() != null &&
                                                        dto.getOwner().getLastName().toLowerCase()
                                                                        .contains(lastName.toLowerCase()))
                                        .collect(Collectors.toList());
                }

                accountDtos.sort(Comparator
                                .comparing(dto -> dto.getOwner() != null && dto.getOwner().getLastName() != null
                                                ? dto.getOwner().getLastName()
                                                : ""));

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), accountDtos.size());
                List<AccountDto> pageContent = accountDtos.subList(start, end);
                return new PageImpl<>(pageContent, pageable, accountDtos.size());
        }

        public void createNewBankAccount(NewBankAccountDto newBankAccountDto, String authorizationHeader) {
                Long userId = newBankAccountDto.getClientId();
                ClientDto clientDto = userClient.getClientById(userId);
                if (clientDto == null)
                        throw new ClientNotFoundException(userId);
                Account newAccount;
                if (newBankAccountDto.getAccountType().equals(AccountOwnerType.COMPANY.toString())) {
                        newAccount = new CompanyAccount();
                        ((CompanyAccount) newAccount).setCompanyId(newBankAccountDto.getCompanyId());
                } else
                        newAccount = new PersonalAccount();

                newAccount.setClientId(newBankAccountDto.getClientId());
                newAccount.setCreatedByEmployeeId(newBankAccountDto.getEmployeeId());
                newAccount.setCreationDate(LocalDate.ofEpochDay(Instant.now().getEpochSecond()));
                System.out.println(newBankAccountDto.getCurrency());
                Currency currCurrency = currencyRepository.findByCode(newBankAccountDto.getCurrency())
                                .orElseThrow(() -> new CurrencyNotFoundException(newBankAccountDto.getCurrency()));
                newAccount.setCurrency(currCurrency);
                newAccount.setStatus(AccountStatus.valueOf(newBankAccountDto.getIsActive()));
                newAccount.setType(AccountType.valueOf(newBankAccountDto.getAccountType()));
                newAccount.setAccountOwnerType(AccountOwnerType.valueOf(newBankAccountDto.getAccountOwnerType()));
                newAccount.setBalance(newBankAccountDto.getInitialBalance());
                newAccount.setAvailableBalance(newBankAccountDto.getInitialBalance());
                newAccount.setDailyLimit(newBankAccountDto.getDailyLimit());
                newAccount.setMonthlyLimit(newBankAccountDto.getMonthlyLimit());
                newAccount.setDailySpending(newBankAccountDto.getDailySpending());
                newAccount.setMonthlySpending(newBankAccountDto.getMonthlySpending());

                String random = String.format("%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
                String accountOwnerTypeNumber = "";
                switch (newBankAccountDto.getAccountOwnerType()) {
                        case "PERSONAL" -> accountOwnerTypeNumber = "11";
                        case "COMPANY" -> accountOwnerTypeNumber = "12";
                        case "SAVINGS" -> accountOwnerTypeNumber = "13";
                        case "RETIREMENT" -> accountOwnerTypeNumber = "14";
                        case "YOUTH" -> accountOwnerTypeNumber = "15";
                        case "STUDENT" -> accountOwnerTypeNumber = "16";
                        case "UNEMPLOYED" -> accountOwnerTypeNumber = "17";
                }

                String accountNumber = "3330001" + random + accountOwnerTypeNumber;
                newAccount.setAccountNumber(accountNumber);

                accountRepository.save(newAccount);
        }

    public List<AccountDto> getMyAccounts(String authorizationHeader) {
        try {
            ClientDto clientDto = userClient.getClient(authorizationHeader);

            return accountRepository.findAllByClientId(clientDto.getId()).stream().map(account ->
                    AccountMapper.toDto(account, clientDto)).sorted(Comparator.comparing(AccountDto::getAvailableBalance,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());
        } catch (FeignException.NotFound e){
            throw new UserNotAClientException();
        }
    }

    public AccountDetailsDto getAccountDetails(String authorizationHeader, String accountNumber) {
        try {
            ClientDto clientDto = userClient.getClient(authorizationHeader);
            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(AccountNotFoundException::new);

            if (!clientDto.getId().equals(account.getClientId()))
                throw new ClientNotAccountOwnerException();

            AccountDetailsDto accountDetailsDto;

            if (account.getAccountOwnerType() != AccountOwnerType.COMPANY){
                accountDetailsDto =  AccountMapper.toDetailsDto(account);
                accountDetailsDto.setAccountOwner(clientDto.getFirstName() + " " + clientDto.getLastName());
            } else {
                accountDetailsDto = AccountMapper.toCompanyDetailsDto(account);
                CompanyAccountDetailsDto companyAccountDetailsDto = (CompanyAccountDetailsDto) accountDetailsDto;

                CompanyAccount companyAccount = (CompanyAccount) account;
                CompanyDto companyDto = userClient.getCompanyById(authorizationHeader, companyAccount.getCompanyId());

                companyAccountDetailsDto.setCompanyName(companyDto.getName());
                companyAccountDetailsDto.setRegistrationNumber(companyDto.getRegistrationNumber());
                companyAccountDetailsDto.setTaxId(companyDto.getTaxId());
                companyAccountDetailsDto.setAddress(companyDto.getAddress());
            }
            return accountDetailsDto;
        } catch (FeignException.NotFound e){
            throw new UserNotAClientException();
        }
    }

    //Verovatno ce da ide u TransactionService ali ne znam jer nemam Transaction entitet
    public void getAccountTransactions(String authorizationHeader, String accountNumber) {
        try {
            ClientDto clientDto = userClient.getClient(authorizationHeader);
            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(AccountNotFoundException::new);

            if (!clientDto.getId().equals(account.getClientId()))
                throw new ClientNotAccountOwnerException();

            //Nemam entitet za transakciju tako da ovde stajem
        } catch (FeignException.NotFound e){
            throw new UserNotAClientException();
        }
    }
}

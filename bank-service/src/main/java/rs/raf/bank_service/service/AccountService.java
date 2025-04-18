package rs.raf.bank_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.repository.CompanyAccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.specification.AccountSearchSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AccountService {
    private final CurrencyRepository currencyRepository;
    private final CompanyAccountRepository companyAccountRepository;
    private final AccountRepository accountRepository;
    private final ChangeLimitRequestRepository changeLimitRequestRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final ExchangeRateService exchangeRateService;
    @Autowired
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    public Page<AccountDto> getBankAccounts(Pageable pageable) {
        return companyAccountRepository.findByCompanyId(1L, pageable).map((account) -> AccountMapper.toDto(account, null));
    }

    public Page<AccountDto> getAccounts(String accountNumber, String firstName, String lastName, Pageable pageable) {

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

    public Page<AccountDto> getAccountsForClient(String accountNumber, Long clientId, Pageable pageable) {
        ClientDto client = userClient.getClientById(clientId);

        Specification<Account> spec = Specification
                .where(AccountSearchSpecification.accountNumberContains(accountNumber)
                        .and(AccountSearchSpecification.clientIs(clientId)));
        Page<Account> accounts = accountRepository.findAll(spec, pageable);

        return accounts.map(account -> AccountMapper.toDto(account, client));
    }

    // Za Cto
    public AccountDto getUSDAccountForClient(Long clientId) {
        ClientDto client = userClient.getClientById(clientId);

        List<Account> accounts = accountRepository.findAllByClientIdAndCurrency_Code(
                clientId, "USD"
        );

        Optional<Account> highestBalance = accounts.stream().max(Comparator.comparing(Account::getAvailableBalance));

        return highestBalance.map(account -> AccountMapper.toDto(account, client)).orElse(null);

    }


    public AccountDto createNewBankAccount(NewBankAccountDto newBankAccountDto, String authorizationHeader) {
        Long employeeId = jwtTokenUtil.getUserIdFromAuthHeader(authorizationHeader);
        Long userId = newBankAccountDto.getClientId();
        ClientDto clientDto = userClient.getClientById(userId);
        if (clientDto == null)
            throw new ClientNotFoundException(userId);
        Account newAccount;
        if (newBankAccountDto.getAccountType().equals(AccountOwnerType.COMPANY.toString())) {
            newAccount = new CompanyAccount();
            ((CompanyAccount) newAccount).setCompanyId(newBankAccountDto.getCompanyId());
            ((CompanyAccount) newAccount).setAuthorizedPersonId(newBankAccountDto.getAuthorizedPersonId());
        } else
            newAccount = new PersonalAccount();

        newAccount.setClientId(newBankAccountDto.getClientId());
        newAccount.setCreatedByEmployeeId(employeeId);
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
        newAccount.setName(newBankAccountDto.getName());

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

        return AccountMapper.toDto(accountRepository.save(newAccount), clientDto);
    }

    public List<AccountDto> getMyAccounts(Long clientId) {
        try {
            ClientDto clientDto = userClient.getClientById(clientId);

            return accountRepository.findAllByClientId(clientId).stream().map(account ->
                    AccountMapper.toDto(account, clientDto)).sorted(Comparator.comparing(AccountDto::getAvailableBalance,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());
        } catch (FeignException.NotFound e) {
            throw new UserNotAClientException();
        }
    }

    public List<AccountDto> getMyUSDAccounts(Long clientId) {
        try {
            ClientDto clientDto = userClient.getClientById(clientId);

            return accountRepository.findAllByClientIdAndCurrency_Code(clientId, "USD").stream().map(account ->
                    AccountMapper.toDto(account, clientDto)).sorted(Comparator.comparing(AccountDto::getAvailableBalance,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());
        } catch (FeignException.NotFound e) {
            throw new UserNotAClientException();
        }
    }

    public AccountDetailsDto getAccountDetails(String role, Long clientId, String accountNumber) {
        try {
            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(AccountNotFoundException::new);

            if (role.equals("CLIENT")) {
                ClientDto caller = userClient.getClientById(clientId);

                if (!caller.getId().equals(account.getClientId()))
                    throw new ClientNotAccountOwnerException();
            }

            ClientDto clientDto = userClient.getClientById(account.getClientId());

            AccountDetailsDto accountDetailsDto;

            if (account.getAccountOwnerType() != AccountOwnerType.COMPANY) {
                accountDetailsDto = AccountMapper.toDetailsDto(account);
                accountDetailsDto.setAccountOwner(clientDto.getFirstName() + " " + clientDto.getLastName());
            } else {
                CompanyAccount companyAccount = (CompanyAccount) account;
                CompanyDto companyDto = userClient.getCompanyById(companyAccount.getCompanyId());

                // Dohvati ovlašćenog korisnika ako postoji
                AuthorizedPersonelDto authorizedPersonnelDto = null;
                if (companyAccount.getAuthorizedPersonId() != null) {
                    authorizedPersonnelDto = userClient.getAuthorizedPersonnelById(companyAccount.getAuthorizedPersonId());
                }

                //  Prosledimo dodatni parametar u mapper
                CompanyAccountDetailsDto companyAccountDetailsDto = AccountMapper.toCompanyDetailsDto(
                        companyAccount,
                        companyDto,
                        authorizedPersonnelDto
                );

                companyAccountDetailsDto.setAccountOwner(clientDto.getFirstName() + " " + clientDto.getLastName());
                companyAccountDetailsDto.setCompanyName(companyDto.getName());
                companyAccountDetailsDto.setRegistrationNumber(companyDto.getRegistrationNumber());
                companyAccountDetailsDto.setTaxId(companyDto.getTaxId());
                companyAccountDetailsDto.setAddress(companyDto.getAddress());

                accountDetailsDto = companyAccountDetailsDto;
            }

            return accountDetailsDto;
        } catch (FeignException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }


    public void changeAccountName(String accountNumber, String newName, String authHeader) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        Account account = accountRepository.findByAccountNumberAndClientId(accountNumber, clientId)
                .orElseThrow(() -> new AccNotFoundException("Account not found"));

        System.out.println(">>> Account found: Current Name = " + account.getName());

        // Proveravamo postoji li ime već u bazi
        boolean exists = accountRepository.existsByNameAndClientId(newName, account.getClientId());
        System.out.println(">>> Checking if account name '" + newName + "' already exists for client ID " + account.getClientId() + ": " + exists);

        if (exists) {
            System.out.println(">>> ERROR: Account name '" + newName + "' is already in use for client ID " + account.getClientId());
            throw new DuplicateAccountNameException("Account name already in use");
        }

        System.out.println(">>> Changing account name from '" + account.getName() + "' to '" + newName + "'");
        account.setName(newName);
        accountRepository.save(account);

        System.out.println(">>> SUCCESS: Account name changed to '" + newName + "'");
    }

    public void requestAccountLimitChange(String accountNumber, BigDecimal newLimit, String authHeader) throws JsonProcessingException {
        if (newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        Account account = accountRepository.findByAccountNumberAndClientId(accountNumber, clientId)
                .orElseThrow(() -> new AccNotFoundException("Account not found"));


        if (!account.getClientId().equals(clientId)) {
            throw new IllegalStateException("This account does not belong to the authenticated client.");
        }


        ChangeLimitRequest request = new ChangeLimitRequest(accountNumber, newLimit);
        changeLimitRequestRepository.save(request);


        ChangeAccountLimitDetailsDto changeAccountLimitDetailsDto = ChangeAccountLimitDetailsDto.builder()
                .accountNumber(account.getAccountNumber())
                .oldLimit(account.getDailyLimit())
                .newLimit(newLimit)
                .build();


        CreateVerificationRequestDto verificationRequest = CreateVerificationRequestDto.builder()
                .userId(clientId)
                .targetId(request.getId())
                .verificationType(VerificationType.CHANGE_LIMIT)
                .details(objectMapper.writeValueAsString(changeAccountLimitDetailsDto))
                .build();

        userClient.createVerificationRequest(verificationRequest);

        log.info("Verification request created for client {}. Please approve to proceed.", clientId);
    }


    public void changeAccountLimit(Long requestId) {

        ChangeLimitRequest request = changeLimitRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new IllegalStateException("No pending limit change request found"));


        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccNotFoundException("Account not found"));


        account.setDailyLimit(request.getNewLimit());
        accountRepository.save(account);


        request.setStatus(VerificationStatus.APPROVED);
        changeLimitRequestRepository.save(request);


        log.info("Account limit updated successfully for account {}", account.getAccountNumber());
    }
    public void rejectAccountLimitChange(Long requestId){
        ChangeLimitRequest changeLimitRequest = changeLimitRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new ChangeLimitReqNotFoundException(requestId));
        if (!changeLimitRequest.getStatus().equals(VerificationStatus.PENDING))
            throw new RejectNonPendingRequestException();
        changeLimitRequest.setStatus(VerificationStatus.DENIED);
        changeLimitRequestRepository.save(changeLimitRequest);
    }

    public void setAuthorizedPerson(Long accountId, Long authorizedPersonId, Long employeeId) {

        System.out.println("Pozvana metoda setAuthorizedPerson sa companyAccountId: " + accountId +
                ", authorizedPersonId: " + authorizedPersonId + ", employeeId: " + employeeId);


        CompanyAccount companyAccount = (CompanyAccount) accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);

        System.out.println("Pronađen račun: " + companyAccount.getAccountNumber());

        //  Proveri da li je korisnik vlasnik firme
        CompanyDto companyDto = userClient.getCompanyById(companyAccount.getCompanyId());
        if (!companyDto.getMajorityOwner().getId().equals(employeeId)) {
            throw new UnauthorizedException("Only the company owner can set an authorized person.");
        }
        System.out.println("Korisnik sa id-em: " + employeeId + " je vlasnik firme.");

        //  Provera da li ovlašćeno lice pripada istoj firmi
        List<AuthorizedPersonelDto> personnelList = userClient.getAuthorizedPersonnelByCompany(companyAccount.getCompanyId());
        AuthorizedPersonelDto authorizedPerson = personnelList.stream()
                .filter(person -> person.getId().equals(authorizedPersonId))
                .findFirst()
                .orElseThrow(InvalidAuthorizedPersonException::new);

        System.out.println("Pronađeno ovlašćeno lice: " + authorizedPerson.getFirstName() + " " + authorizedPerson.getLastName());

        //  Postavi authorizedPersonId i sačuvaj
        companyAccount.setAuthorizedPersonId(authorizedPerson.getId());
        accountRepository.save(companyAccount);

        System.out.println("Uspešno postavljeno ovlašćeno lice " + authorizedPerson.getFirstName() + " za račun " + accountId);

    }

    public BigDecimal getAccountBalance(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccNotFoundException("Account not found"));

        return account.getBalance(); //vidi da li treba balance ili availabe balance
    }

    public List<AccountDto> getAllBankAccounts() {
        List<CompanyAccount> bankAccounts = companyAccountRepository
                .findByCompanyId(1L, Pageable.unpaged()).getContent();
        List<AccountDto> bankAccountDtos = bankAccounts.stream()
                .map(account -> AccountMapper.toDto(account, null))
                .toList();

        List<AccountDto> allAccounts = new ArrayList<>();
        allAccounts.addAll(bankAccountDtos);
        allAccounts.sort(Comparator.comparing(AccountDto::getAccountNumber));

        return allAccounts;
    }

    public List<AccountDto> getBankUSDAccounts() {
        return companyAccountRepository
                .findByCompanyIdAndCurrency_Code(1L, "USD")
                .stream()
                .map(account -> AccountMapper.toDto(account, null))
                .toList();
    }


    public void updateAvailableBalance(String accountNumber, BigDecimal amount){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(AccountNotFoundException::new);

        if(!account.getCurrency().getCode().equals("USD")){
            amount = convertToAccountCurrency(account.getCurrency().getCode(), amount);
        }

        BigDecimal newAvailableBalance = account.getAvailableBalance().add(amount);

        if (newAvailableBalance.compareTo(BigDecimal.ZERO) < 0)
            throw new InsufficientFundsException(account.getAvailableBalance(), amount.multiply(BigDecimal.valueOf(-1)));

        account.setAvailableBalance(newAvailableBalance);
        accountRepository.save(account);
    }

    public void updateBalance(String accountNumber, BigDecimal amount){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(AccountNotFoundException::new);

        if(!account.getCurrency().getCode().equals("USD")){
           amount = convertToAccountCurrency(account.getCurrency().getCode(), amount);
        }

        BigDecimal newBalance = account.getBalance().add(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0)
            throw new InsufficientFundsException(account.getBalance(), amount.multiply(BigDecimal.valueOf(-1)));

        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    private BigDecimal convertToAccountCurrency(String toCurrency, BigDecimal amount) {
        ExchangeRateDto exchangeRateDto1 = exchangeRateService.getExchangeRate("USD", "RSD");
        amount = amount.multiply(exchangeRateDto1.getExchangeRate());

        if(!toCurrency.equals("RSD")){
            ExchangeRateDto exchangeRateDto2 = exchangeRateService.getExchangeRate("USD", toCurrency);
            amount = amount.multiply(exchangeRateDto2.getExchangeRate());
        }

        return amount;
    }
}

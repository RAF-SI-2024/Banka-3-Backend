package rs.raf.bank_service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.controller.CardController;
import rs.raf.bank_service.controller.PaymentController;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CardRequest;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardType;
import rs.raf.bank_service.domain.enums.RequestStatus;
import rs.raf.bank_service.exceptions.PaymentNotFoundException;
import rs.raf.bank_service.exceptions.ReceiverAccountNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRequestRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class BankServiceTestsSteps extends BankServiceTestsConfig {

    @Autowired
    UserClient userClient;

    @Autowired
    AccountController accountController;

    @Autowired
    CardController cardController;

    @Autowired
    PaymentController paymentController;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    CardRequestRepository cardRequestRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    private String employeeToken;
    private String clientToken;
    private ClientDto clientDto;
    private ClientDto client;
    private List<AccountDto> accounts;
    private CreateCardDto dto;
    private CardDtoNoOwner cardDto;
    private BigDecimal senderInitialBalance;
    private BigDecimal recieverInitialBalance;
    private Long payment;

    public void authenticateWithJwtEmployee(String authHeader, JwtTokenUtil jwtTokenUtil) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid or missing Authorization header");
        }

        String token = authHeader.replace("Bearer ", "").trim();

        Claims claims = jwtTokenUtil.getClaimsFromToken(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void authenticateWithJwtClient(String authHeader, JwtTokenUtil jwtTokenUtil) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid or missing Authorization header");
        }

        String token = authHeader.replace("Bearer ", "").trim();

        Claims claims = jwtTokenUtil.getClaimsFromToken(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void authenticateWithJwtAdmin(String authHeader, JwtTokenUtil jwtTokenUtil) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid or missing Authorization header");
        }

        String token = authHeader.replace("Bearer ", "").trim();

        Claims claims = jwtTokenUtil.getClaimsFromToken(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @Given("an employee with email {string} and password {string} logs in")
    public void anEmployeeWithEmailAndPasswordLogsIn(String arg0, String arg1) {
        LoginRequestDto loginRequestDto = new LoginRequestDto();
        loginRequestDto.setEmail(arg0);
        loginRequestDto.setPassword(arg1);
        LoginResponseDto loginResponseDto = userClient.employeeLogin(loginRequestDto);
        employeeToken = loginResponseDto.getToken();
    }

    @When("an employee creates a client with email {string}")
    public void anEmployeeCreatesAClientWithEmail(String arg0) {
        CreateClientDto createClientDto = new CreateClientDto();
        createClientDto.setFirstName("Jovana");
        createClientDto.setLastName("Djukic");
        createClientDto.setGender("F");
        createClientDto.setAddress("Tu negde u Beogradu");
        createClientDto.setUsername("j.djukic");
        createClientDto.setPhone("0640640640");
        createClientDto.setEmail(arg0);
        createClientDto.setJmbg("0106003710059");
        String dateString = "2003-06-01";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date;
        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        createClientDto.setBirthDate(date);

        authenticateWithJwtEmployee("Bearer " + employeeToken, jwtTokenUtil);
        clientDto = userClient.addClient(createClientDto);
    }


    @WithMockUser(username = "petar.p@example.com", roles = {"ADMIN"})
    @And("an employee creates an account for the client with email {string}")
    public void anEmployeeCreatesAnAccountForTheClientWithEmail(String arg0) {
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(clientDto.getId());
        newBankAccountDto.setAccountType("CURRENT");
        newBankAccountDto.setEmployeeId(jwtTokenUtil.getUserIdFromAuthHeader("Bearer " + employeeToken));
        newBankAccountDto.setAccountOwnerType("STUDENT");
        newBankAccountDto.setCompanyId(null);
        newBankAccountDto.setInitialBalance(BigDecimal.valueOf(3000L));
        newBankAccountDto.setDailyLimit(BigDecimal.valueOf(20000L));
        newBankAccountDto.setMonthlyLimit(BigDecimal.valueOf(1000000L));
        newBankAccountDto.setDailySpending(BigDecimal.valueOf(20000L));
        newBankAccountDto.setMonthlySpending(BigDecimal.valueOf(1000000L));
        newBankAccountDto.setIsActive("ACTIVE");
        newBankAccountDto.setCreateCard(false);
        newBankAccountDto.setCurrency("RSD");

        authenticateWithJwtEmployee("Bearer " + employeeToken, jwtTokenUtil);
        accountController.createBankAccount("Bearer " + employeeToken, newBankAccountDto);

    }

    @Then("when searching for client with email {string} it comes up")
    public void whenSearchingForClientWithEmailItComesUp(String arg0) {
        client = userClient.getClientById(clientDto.getId());
        if (client.getEmail().equals(arg0)) {
            return;
        }
        fail("Did not find client with email " + arg0);
    }


    @And("when all accounts are listed for client with email {string} list is not empty")
    public void whenAllAccountsAreListedForClientWithEmailAccountWithComesUp(String arg0) {
        Page<AccountDto> pageAccounts = (Page<AccountDto>) accountController.getAccountsForClient(null, clientDto.getId(), 0, 10).getBody();
        if (pageAccounts == null) {
            fail("Failed to recover page of account dtos");
        }

        accounts = pageAccounts.getContent(); // Zamena toList() sa getContent()

        if (accounts.isEmpty()) {
            fail("No accounts were created for client with email " + arg0);
        }
    }

    @When("the client sets {string} as password")
    public void theClientSetsAPassword(String arg0) {
        ActivationRequestDto activationRequestDto = new ActivationRequestDto();
        activationRequestDto.setToken("df7ff5f0-70bd-492c-9569-ac5f3fbda7ff");  //hardcoded token, added in user service bootstrap
        activationRequestDto.setPassword(arg0);
        userClient.activateUser(activationRequestDto);
    }


    @And("the client with email {string} and password {string} logs in")
    public void theClientWithEmailAndPasswordLogsIn(String arg0, String arg1) {
        LoginRequestDto loginRequestDto = new LoginRequestDto();
        loginRequestDto.setEmail(arg0);
        loginRequestDto.setPassword(arg1);
        LoginResponseDto loginResponseDto = userClient.clientLogin(loginRequestDto);
        clientToken = loginResponseDto.getToken();
    }

    @And("the client requests a new card")
    public void theClientRequestsANewCard() throws JsonProcessingException {
        CreateCardDto createCardDto = new CreateCardDto();
        createCardDto.setType(CardType.DEBIT);
        createCardDto.setName("Mastercard");
        createCardDto.setCardLimit(BigDecimal.valueOf(1000000L));
        Page<AccountDto> accountsPage = (Page<AccountDto>) accountController.getAccountsForClient(null, clientDto.getId(), 0, 10).getBody();
        if (accountsPage == null || accountsPage.getContent().isEmpty()) {
            fail("No accounts found for client: " + clientDto.getId());
        }
        String accountNumber = accountsPage.getContent().get(0).getAccountNumber();
        createCardDto.setAccountNumber(accountNumber);
        createCardDto.setIssuer(CardIssuer.MASTERCARD);

        dto = createCardDto;

        authenticateWithJwtClient("Bearer " + clientToken, jwtTokenUtil);
        cardController.requestNewCard(createCardDto, "Bearer " + clientToken);
    }

    @And("the employee confirms card creation request and the card is created")
    public void theEmployeeConfirmsCardCreationRequestAndTheCardIsCreated() {
        authenticateWithJwtAdmin("Bearer " + employeeToken, jwtTokenUtil);
        List<CardRequest> pendingRequests = cardRequestRepository.findByAccountNumberAndStatus(
                dto.getAccountNumber(), RequestStatus.PENDING);

        if (pendingRequests.isEmpty()) {
            fail("No pending card requests found");
        }

        Long cardRequestId = pendingRequests.get(0).getId();
        cardController.approveCardRequest(cardRequestId);
    }


    @Then("when all cards are listed for account, the list is not empty")
    public void whenAllCardsAreListedForAccountTheListIsNotEmpty() {
        authenticateWithJwtEmployee("Bearer " + employeeToken, jwtTokenUtil);
        Object response = cardController.getCardsByAccount(dto.getAccountNumber()).getBody();

        if (response == null) {
            fail("Failed to recover cards for account: " + dto.getAccountNumber());
        }

        List<CardDto> listOfCards;
        if (response instanceof List<?>) {
            listOfCards = (List<CardDto>) response; // Eksplicitni kast ako getBody() vraća Object
        } else {
            fail("Unexpected response type for getCardsByAccount");
            return;
        }

        if (listOfCards.isEmpty()) {
            fail("No cards were created for account: " + dto.getAccountNumber());
        }
    }

    @When("the client initiates a payment to another bootstrap account")
    public void theClientInitiatesAPaymentToAnotherBootstrapAccount() {
        CreatePaymentDto createPaymentDto = new CreatePaymentDto();
        createPaymentDto.setPaymentCode("222");
        createPaymentDto.setAmount(BigDecimal.valueOf(200L));
        createPaymentDto.setPurposeOfPayment("Isplata caciju za blejanje u Pionirskom parku");
        createPaymentDto.setReferenceNumber("");
        createPaymentDto.setSenderAccountNumber(dto.getAccountNumber());
        createPaymentDto.setReceiverAccountNumber("333111111111111112");

        senderInitialBalance = accountRepository.findByAccountNumber(dto.getAccountNumber()).get().getBalance();
        recieverInitialBalance = accountRepository.findByAccountNumber("333111111111111112").get().getBalance();

        authenticateWithJwtClient("Bearer " + clientToken, jwtTokenUtil);
        paymentController.newPayment(createPaymentDto, "Bearer " + clientToken);
    }

    @Then("the money has not been sent yet")
    public void theMoneyHasNotBeenSentYet() {
        if (!senderInitialBalance.equals(accountRepository.findByAccountNumber(dto.getAccountNumber()).get().getBalance()) ||
                !recieverInitialBalance.equals(accountRepository.findByAccountNumber("333111111111111112").get().getBalance())) {
            fail("Balance has been transfered before the payment was confirmed.");
        }
    }

    @When("the admin approves the payment")
    public void theAdminApprovesThePayment() {
        List<PaymentOverviewDto> payments = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            payments = paymentController.getPayments("Bearer " + clientToken, null, null, null, null,
                    null, dto.getAccountNumber(), null, 0, 10).getBody().toList();

            if (payments.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Long paymentId = payments.get(0).getId();
        payment = paymentId;
        authenticateWithJwtAdmin("Bearer " + employeeToken, jwtTokenUtil);
        paymentController.confirmPayment(paymentId);
    }

    @Then("the money is successfully transferred")
    public void theMoneyIsSuccessfullyTransferred() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        BigDecimal newSenderBalance = accountRepository.findByAccountNumber(dto.getAccountNumber()).get().getBalance();
        BigDecimal newReceiverBalance = accountRepository.findByAccountNumber("333111111111111112").get().getBalance();

        System.out.println(senderInitialBalance);
        System.out.println(recieverInitialBalance);

        System.out.println(newSenderBalance);
        System.out.println(newReceiverBalance);

        if (!senderInitialBalance.subtract(BigDecimal.valueOf(200L)).equals(newSenderBalance) ||
                !recieverInitialBalance.add(BigDecimal.valueOf(200L)).equals(newReceiverBalance)) {
            fail("Payment has failed.");
        }
    }

}

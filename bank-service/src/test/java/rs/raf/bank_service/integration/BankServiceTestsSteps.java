package rs.raf.bank_service.integration;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
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
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
        accounts = accountController.getAccountsForClient(null, clientDto.getId(), 0, 10).getBody().toList();
        if (accounts == null) {
            fail("Failed to recover page of account dtos");
        } else if (accounts.isEmpty()) {
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
    public void theClientRequestsANewCard() {
        CreateCardDto createCardDto = new CreateCardDto();
        createCardDto.setType("DEBIT");
        createCardDto.setName("Mastercard");
        createCardDto.setCardLimit(BigDecimal.valueOf(1000000L));
        String accountNumber = accountController.getAccountsForClient(null, clientDto.getId(), 0, 10)
                .getBody().toList().get(0).getAccountNumber();
        createCardDto.setAccountNumber(accountNumber);
        dto = createCardDto;

        authenticateWithJwtClient("Bearer " + clientToken, jwtTokenUtil);
        cardController.requestCardForAccount(createCardDto);
    }

    @And("the client confirms card creation request using token sent to his email and the card is created")
    public void theClientConfirmsCardCreationRequestUsingTokenSentToHisEmailAndTheCardIsCreated() {
        CardRequestDto cardRequestDto = new CardRequestDto();
        cardRequestDto.setToken("df7ff5f0-70bd-492c-9569-ac5f3fbda7xd");    //hardcoded token, added in user service bootstrap
        cardRequestDto.setCreateCardDto(dto);

        cardDto = cardController.verifyAndReceiveCard(cardRequestDto).getBody();
    }


    @Then("when all cards are listed for account, the list is not empty")
    public void whenAllCardsAreListedForAccountTheListIsNotEmpty() {
        authenticateWithJwtEmployee("Bearer " + employeeToken, jwtTokenUtil);
        List<CardDto> listOfCards = cardController.getCardsByAccount(cardDto.getAccountNumber()).getBody();
        if (listOfCards == null) {
            fail("Failed to recover cards for account: " + cardDto.getAccountNumber());
        } else if (listOfCards.isEmpty()) {
            fail("No cards were created for account: " + cardDto.getAccountNumber());
        }
    }

    @When("the client initiates a payment to another bootstrap account")
    public void theClientInitiatesAPaymentToAnotherBootstrapAccount() {
        CreatePaymentDto createPaymentDto = new CreatePaymentDto();
        createPaymentDto.setPaymentCode("222");
        createPaymentDto.setAmount(BigDecimal.valueOf(500L));
        createPaymentDto.setPurposeOfPayment("Isplata caciju za blejanje u Pionirskom parku");
        createPaymentDto.setReferenceNumber("");
        createPaymentDto.setSenderAccountNumber(cardDto.getAccountNumber());
        createPaymentDto.setReceiverAccountNumber("111111111111111111");

        senderInitialBalance = accountRepository.findByAccountNumber(cardDto.getAccountNumber()).get().getBalance();
        recieverInitialBalance = accountRepository.findByAccountNumber("111111111111111111").get().getBalance();

        authenticateWithJwtClient("Bearer " + clientToken, jwtTokenUtil);
        paymentController.newPayment(createPaymentDto, "Bearer " + clientToken);
    }

    @Then("the money has not been sent yet")
    public void theMoneyHasNotBeenSentYet() {
        if (!senderInitialBalance.equals(accountRepository.findByAccountNumber(cardDto.getAccountNumber()).get().getBalance()) ||
                !recieverInitialBalance.equals(accountRepository.findByAccountNumber("111111111111111111").get().getBalance())) {
            fail("Balance has been transfered before the payment was confirmed.");
        }
    }

    @When("the admin approves the payment")
    public void theAdminApprovesThePayment() {
        Long paymentId = paymentController.getPayments("Bearer " + clientToken, null, null, null, null,
                null, cardDto.getAccountNumber(), null, 0, 10).getBody().toList().get(0).getId();

        authenticateWithJwtAdmin("Bearer " + employeeToken, jwtTokenUtil);
        paymentController.confirmPayment(paymentId);
    }

    @Then("the money is successfully transferred")
    public void theMoneyIsSuccessfullyTransferred() {
        if (!senderInitialBalance.equals(accountRepository.findByAccountNumber(cardDto.getAccountNumber()).get().getBalance().add(BigDecimal.valueOf(500L))) ||
                !recieverInitialBalance.equals(accountRepository.findByAccountNumber("111111111111111111").get().getBalance().subtract(BigDecimal.valueOf(500L)))) {
            fail("Payment has failed.");
        }
    }

}

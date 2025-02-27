package rs.raf.user_service.integration.userservice.services;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.user_service.dto.*;
import rs.raf.user_service.integration.userservice.UserServiceTestsConfig;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.service.AuthService;
import rs.raf.user_service.service.ClientService;
import rs.raf.user_service.service.EmployeeService;
import rs.raf.user_service.service.UserService;
import rs.raf.user_service.utils.JwtTokenUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Transactional
@ExtendWith(MockitoExtension.class)
public class UserServiceTestsSteps extends UserServiceTestsConfig {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    List<ClientDto> clients;
    ClientDto addedClient;
    ClientDto foundClient;
    List<EmployeeDto> employees;
    EmployeeDto addedEmployee;
    EmployeeDto foundEmployee;
    String jwt;
    JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();
    @Autowired
    AuthTokenRepository authTokenRepository;
    @Autowired
    private ClientService clientService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @When("created a new client with first name {string}, second name {string}, email {string}, adress {string}, phone number {string}, gender {string}, and birthday on {string}")
    public void createdANewClientWithFirstNameSecondNameEmailAdressPhoneNumberGenderAndBirthdayOn(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6) {
        CreateClientDto newClient = new CreateClientDto();

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        String dateStr = arg6;
        Date date = new Date();
        try {
            date = formatter.parse(dateStr);
        } catch (ParseException e) {
            fail(e.getMessage());
        }

        newClient.setFirstName(arg0);
        newClient.setLastName(arg1);
        newClient.setEmail(arg2);
        newClient.setAddress(arg3);
        newClient.setPhone(arg4);
        newClient.setGender(arg5);
        newClient.setBirthDate(date);

        addedClient = clientService.addClient(newClient);
    }

    @And("list all clients")
    public void listAllClients() {
        clients = clientService.listClients(Pageable.unpaged()).getContent();
    }

    @Then("recieve client with email {string}")
    public void recieveClientWithEmail(String arg0) {
        for (ClientDto client : clients) {
            if (client.getEmail().equals(arg0)) {
                return;
            }
        }
        fail("There is no client with the email: " + arg0);
    }

    @When("updating the clients phone number with {string}")
    public void updatingTheClientsPhoneNumberWith(String arg0) {
        UpdateClientDto updateClientDto = new UpdateClientDto();
        updateClientDto.setLastName(addedClient.getLastName());
        updateClientDto.setGender(addedClient.getGender());
        updateClientDto.setAddress(addedClient.getAddress());
        updateClientDto.setPhone(arg0);

        addedClient = clientService.updateClient(addedClient.getId(), updateClientDto);
    }

    @And("searching for that client")
    public void searchingForThatClient() {
        foundClient = clientService.getClientById(addedClient.getId());
        if (foundClient == null) {
            fail("Did not find a client with that id.");
        }
    }

    @Then("recieve that client with email {string} and phone number {string}")
    public void recieveThatClientWithEmailAndPhoneNumber(String arg0, String arg1) {
        if (!foundClient.getEmail().equals(arg0)) {
            fail("Email does not match.");
        }
        if (!foundClient.getPhone().equals(arg1)) {
            fail("Phone number not updated properly.");
        }
    }

    @When("deleting that client")
    public void deletingThatClient() {
        clientService.deleteClient(addedClient.getId());
    }


    @Then("the client with the {string} email does not exist in the list of all clients")
    public void theClientWithTheEmailDoesNotExistInTheListOfAllClients(String arg0) {
        clients = clientService.listClients(Pageable.unpaged()).getContent();
        for (ClientDto client : clients) {
            if (client.getEmail().equals(arg0)) {
                fail("There is a client with email: " + arg0);
            }
        }
    }

    @When("created a new employee with first name {string}, second name {string}, email {string}, adress {string}, phone number {string}, gender {string}, birthday on {string}, username {string}, position {string} and department {string}")
    public void createdANewEmployeeWithFirstNameSecondNameEmailAdressPhoneNumberGenderBirthdayOnUsernamePositionAndDepartment(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8, String arg9) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        String dateStr = arg6;
        Date date = new Date();
        try {
            date = formatter.parse(dateStr);
        } catch (ParseException e) {
            fail(e.getMessage());
        }

        CreateEmployeeDto newEmployee = new CreateEmployeeDto(arg0, arg1, date, arg5, arg2, false, arg4, arg3, arg7, arg8, arg9);
        addedEmployee = employeeService.createEmployee(newEmployee);

    }

    @And("list all employees")
    public void listAllEmployees() {
        employees = employeeService.findAll(addedEmployee.getFirstName(), addedEmployee.getLastName(), addedEmployee.getEmail(), addedEmployee.getPosition(), Pageable.unpaged()).getContent();
    }

    @Then("recieve employee with email {string}")
    public void recieveEmployeeWithEmail(String arg0) {
        for (EmployeeDto employee : employees) {
            if (employee.getEmail().equals(arg0)) {
                return;
            }
        }
        fail("There is no employee with the email: " + arg0);
    }

    @When("activated employee account")
    public void activatedEmployeeAccount() {
        employeeService.activateEmployee(addedEmployee.getId());
    }

    @And("updated employees phone number to {string}")
    public void updatedEmployeesPhoneNumberTo(String arg0) {
        UpdateEmployeeDto updateEmployeeDto = new UpdateEmployeeDto(addedEmployee.getLastName(), addedEmployee.getGender(), arg0, addedEmployee.getAddress(), addedEmployee.getPosition(), addedEmployee.getDepartment());

        employeeService.updateEmployee(addedEmployee.getId(), updateEmployeeDto);
    }


    @And("searching for that employee")
    public void searchingForThatEmployee() {
        foundEmployee = employeeService.findById(addedEmployee.getId());
        if (foundEmployee == null) {
            fail("Did not find employee with that id.");
        }
    }

    @Then("recieve that employee with email {string}, now activated and with phone number {string}")
    public void recieveThatEmployeeWithEmailNowActivatedAndWithPhoneNumber(String arg0, String arg1) {
        if (!foundEmployee.getEmail().equals(arg0)) {
            fail("Email does not match.");
        }
        if (!foundEmployee.isActive()) {
            fail("Employee is not active");
        }
        if (!foundEmployee.getPhone().equals(arg1)) {
            fail("Phone number not updated properly.");
        }
    }

    @When("deleting that employee")
    public void deletingThatEmployee() {
        employeeService.deleteEmployee(addedEmployee.getId());
    }

    @Then("the employee with the {string} email does not exist in the list of all employees")
    public void theEmployeeWithTheEmailDoesNotExistInTheListOfAllEmployees(String arg0) {
        employees = employeeService.findAll(addedEmployee.getFirstName(), addedEmployee.getLastName(), addedEmployee.getEmail(), addedEmployee.getPosition(), Pageable.unpaged()).getContent();
        for (EmployeeDto employee : employees) {
            if (employee.getEmail().equals(arg0)) {
                fail("There is a client with email: " + arg0);
            }
        }
    }

    @Transactional
    @When("added a new permission with id {string}")
    public void addedANewPermissionWithId(String arg0) {
        userService.addPermissionToUser(addedEmployee.getId(), Long.parseLong(arg0));
    }

    @Transactional
    @Then("recieve permission with id {string} when listing all permisions of that user")
    public void recievePermissionWithIdWhenListingAllPermisionsOfThatUser(String arg0) {
        List<PermissionDto> list = userService.getUserPermissions(addedEmployee.getId());
        for (PermissionDto perm : list) {
            if (perm.getId().equals(Long.parseLong(arg0))) {
                return;
            }
        }
        fail("There is no permission with given id when listing all permissions of given user.");
    }

    @Transactional
    @And("removed permission with id {string}")
    public void removedPermissionWithId(String arg0) {
        userService.removePermissionFromUser(addedEmployee.getId(), Long.parseLong(arg0));
    }

    @Transactional
    @Then("not recieve permission with id {string} when listing all permisions of that user")
    public void notRecievePermissionWithIdWhenListingAllPermisionsOfThatUser(String arg0) {
        List<PermissionDto> list = userService.getUserPermissions(addedEmployee.getId());
        for (PermissionDto perm : list) {
            if (perm.getId().equals(Long.parseLong(arg0))) {
                fail("There is a permission attached to the given user when it should not be there.");
            }
        }
    }

    @Given("a client exists with email {string}")
    public void aClientExistsWithEmail(String arg0) {
        //BootstrapData
    }

    @Transactional
    @When("client logs in with email {string} and password {string}")
    public void clientLogsInWithEmailAndPassword(String arg0, String arg1) {
        jwt = authService.authenticateClient(arg0, arg1);
    }

    @Then("the client with email {string} receives a valid JWT token")
    public void theClientReceivesAValidJWTToken(String arg0) {
        assertNotNull(jwt, "JWT token should not be null");
        if (!jwtTokenUtil.getSubjectFromToken(jwt).equals(arg0)) {
            fail("JWT token invalid");
        }
    }


}

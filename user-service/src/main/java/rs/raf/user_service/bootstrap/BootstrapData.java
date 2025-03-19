package rs.raf.user_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.raf.user_service.domain.entity.*;
import rs.raf.user_service.repository.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Component
public class BootstrapData implements CommandLineRunner {
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final PermissionRepository permissionRepository;
    private final ActivityCodeRepository activityCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final ActuaryLimitRepository actuaryLimitRepository;

    @Override
    public void run(String... args) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        if (permissionRepository.count() == 0) {

        }

        if (roleRepository.count() == 0) {
            Role clientRole = Role.builder()
                    .name("CLIENT")
                    .build();
            Role employeeRole = Role.builder()
                    .name("EMPLOYEE")
                    .build();
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .build();
            Role agentRole = Role.builder()
                    .name("AGENT")
                    .build();
            Role supervisorRole = Role.builder()
                    .name("SUPERVISOR")
                    .build();

            roleRepository.saveAll(Arrays.asList(clientRole, employeeRole, adminRole, agentRole, supervisorRole));
        }




        if (clientRepository.count() == 0) {

            Role clientRole = roleRepository.findByName("CLIENT")
                    .orElseThrow(() -> new RuntimeException("Role CLIENT not found"));

            Client client = Client.builder()
                    .firstName("Marko")
                    .lastName("Markovic")
                    .email("marko.m@example.com")
                    .phone("0611158275")
                    .address("Pozeska 56")
                    .birthDate(dateFormat.parse("1990-05-15"))
                    .gender("M")
                    .password(passwordEncoder.encode("markomarko"))
                    .jmbg("0123456789126")
                    .username("marko1")
                    .role(clientRole)
                    .build();

            Client client2 = Client.builder()
                    .firstName("Jovan")
                    .lastName("Jovanovic")
                    .email("jovan.v@example.com")
                    .phone("0671152371")
                    .address("Cara Dusana 105")
                    .birthDate(dateFormat.parse("1990-01-25"))
                    .gender("M")
                    .password(passwordEncoder.encode("jovanjovan"))
                    .jmbg("0123456789125")
                    .username("jovan1")
                    .role(clientRole)
                    .build();

            clientRepository.saveAll(Set.of(client, client2));
        }

        if (employeeRepository.count() == 0) {

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));
            Role employeeRole = roleRepository.findByName("EMPLOYEE")
                    .orElseThrow(() -> new RuntimeException("Role EMPLOYEE not found"));
            Role agentRole = roleRepository.findByName("AGENT")
                    .orElseThrow(() -> new RuntimeException("Role AGENT not found"));

            Employee employee = Employee.builder()
                    .firstName("Petar")
                    .lastName("Petrovic")
                    .email("petar.p@example.com")
                    .phone("0699998279")
                    .address("Trg Republike 5, Beograd")
                    .birthDate(dateFormat.parse("2000-02-19"))
                    .gender("M")
                    .username("petar90")
                    .password(passwordEncoder.encode("petarpetar"))
                    .position("Manager")
                    .department("HR")
                    .active(true)
                    .role(adminRole)
                    .jmbg("0123456789123")
                    .build();

            Employee employee2 = Employee.builder()
                    .firstName("Jana")
                    .lastName("Ivanovic")
                    .email("jana.i@example.com")
                    .phone("0666658276")
                    .address("Palih Boraca 5")
                    .birthDate(dateFormat.parse("1996-09-02"))
                    .gender("F")
                    .username("jana1")
                    .password(passwordEncoder.encode("janajana"))
                    .position("Manager")
                    .department("Finance")
                    .active(true)
                    .jmbg("0123456789124")
                    .role(employeeRole)
                    .build();

            Employee employee3 = Employee.builder()
                    .firstName("Zika")
                    .lastName("Petrović")
                    .email("zika.p@example.com")
                    .phone("0641234567")
                    .address("Kralja Petra 10")
                    .birthDate(dateFormat.parse("1992-05-15"))
                    .gender("M")
                    .username("zika92")
                    .password(passwordEncoder.encode("zikazika"))
                    .position("")
                    .department("IT")
                    .active(true)
                    .jmbg("1505923891234")
                    .role(agentRole)
                    .build();

            employeeRepository.saveAll(Set.of(employee, employee2,employee3));
        }

        if (activityCodeRepository.count() == 0) {

            ActivityCode activityCode = ActivityCode.builder()
                    .id("10.01")
                    .description("Food production")
                    .build();

            activityCodeRepository.save(activityCode);
        }

        if (companyRepository.count() == 0) {
            Company bank = Company.builder()
                    .id(1L)
                    .address("Adresa banke")
                    .name("Banka 3")
                    .activityCode("10.02")
                    .registrationNumber("11111111")
                    .taxId("111111111111111")
                    .majorityOwner(null)
                    .build();

            Company state = Company.builder()
                    .id(2L)
                    .address("Adresa drzave")
                    .name("Republika Srbija")
                    .activityCode("10.02")
                    .registrationNumber("1")
                    .taxId("1")
                    .majorityOwner(null)
                    .build();

            Company company1 = Company.builder()
                    .id(3L)
                    .address("Adresa microsofta")
                    .name("Microsoft")
                    .activityCode("10.01")
                    .registrationNumber("3351361632441")
                    .taxId("36232343")
                    .majorityOwner(clientRepository.findById(1L).get())
                    .build();

            companyRepository.saveAll(List.of(bank, state, company1));
        }

        if (authTokenRepository.count() == 0){
            AuthToken authToken = AuthToken.builder()
                    .token("df7ff5f0-70bd-492c-9569-ac5f3fbda7ff")
                    .type("set-password")
                    .createdAt(1741631004271L)
                    .expiresAt(3000000000000L)
                    .userId(5L)
                    .build();
            authTokenRepository.save(authToken);

            AuthToken authToken1 = AuthToken.builder()
                    .token("df7ff5f0-70bd-492c-9569-ac5f3fbda7xd")
                    .type("request-card")
                    .createdAt(1741631004271L)
                    .expiresAt(3000000000000L)
                    .userId(5L)
                    .build();
            authTokenRepository.save(authToken1);
        }

        if (actuaryLimitRepository.count() == 0) {
            Employee e2 = employeeRepository.findByEmail("zika.p@example.com").orElseThrow();
            ActuaryLimit al2 = new ActuaryLimit(null, new BigDecimal("10000.00"), new BigDecimal("2500.00"), false, e2);

            actuaryLimitRepository.save(al2);

        }

    }
}

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
import java.time.LocalDate;
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
    private final AuthorizedPersonelRepository authorizedPersonelRepository;

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
                    .id(1L)
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
                    .id(2L)
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

            clientRepository.saveAll(List.of(client, client2));
        }

        if (employeeRepository.count() == 0) {

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));
            Role employeeRole = roleRepository.findByName("EMPLOYEE")
                    .orElseThrow(() -> new RuntimeException("Role EMPLOYEE not found"));
            Role agentRole = roleRepository.findByName("AGENT")
                    .orElseThrow(() -> new RuntimeException("Role AGENT not found"));

            Employee employee = Employee.builder()
                    .id(3L)
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
                    .id(4L)
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
                    .id(5L)
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

            employeeRepository.saveAll(List.of(employee, employee2,employee3));
        }

        // CHATGPT CODE START 😊
        if (activityCodeRepository.count() == 0) {
            ActivityCode activityCode = ActivityCode.builder()
                    .id("10.01")
                    .description("Food production")
                    .build();
            ActivityCode activityCode2 = ActivityCode.builder()
                    .id("62.01")
                    .description("Programming")
                    .build();
            ActivityCode activityCode3 = ActivityCode.builder()
                    .id("5.1")
                    .description("Coal extraction")
                    .build();
            ActivityCode activityCode4 = ActivityCode.builder()
                    .id("62.09")
                    .description("IT")
                    .build();
            ActivityCode activityCode5 = ActivityCode.builder()
                    .id("56.1")
                    .description("Restaurants")
                    .build();
            ActivityCode activityCode6 = ActivityCode.builder()
                    .id("86.1")
                    .description("Hospital activities")
                    .build();
            ActivityCode activityCode7 = ActivityCode.builder()
                    .id("90.02")
                    .description("Museums")
                    .build();
            ActivityCode activityCode8 = ActivityCode.builder()
                    .id("1.11")
                    .description("Grain and legume farming")
                    .build();
            ActivityCode activityCode9 = ActivityCode.builder()
                    .id("1.13")
                    .description("Vegetable farming")
                    .build();
            ActivityCode activityCode10 = ActivityCode.builder()
                    .id("13.1")
                    .description("Textile fiber preparation and spinning")
                    .build();
            ActivityCode activityCode11 = ActivityCode.builder()
                    .id("24.1")
                    .description("Iron and steel production")
                    .build();
            ActivityCode activityCode12 = ActivityCode.builder()
                    .id("24.2")
                    .description("Steel pipes, hollow profiles, and fittings production")
                    .build();
            ActivityCode activityCode13 = ActivityCode.builder()
                    .id("41.1")
                    .description("Construction project development")
                    .build();
            ActivityCode activityCode14 = ActivityCode.builder()
                    .id("41.2")
                    .description("Residential and non-residential building construction")
                    .build();
            ActivityCode activityCode15 = ActivityCode.builder()
                    .id("42.11")
                    .description("Road and highway construction")
                    .build();
            ActivityCode activityCode16 = ActivityCode.builder()
                    .id("42.12")
                    .description("Railway and underground construction")
                    .build();
            ActivityCode activityCode17 = ActivityCode.builder()
                    .id("42.13")
                    .description("Bridge and tunnel construction")
                    .build();
            ActivityCode activityCode18 = ActivityCode.builder()
                    .id("42.21")
                    .description("Water supply construction projects")
                    .build();
            ActivityCode activityCode19 = ActivityCode.builder()
                    .id("42.22")
                    .description("Electric power and telecommunications network construction")
                    .build();
            ActivityCode activityCode20 = ActivityCode.builder()
                    .id("7.1")
                    .description("Iron ore extraction")
                    .build();
            ActivityCode activityCode21 = ActivityCode.builder()
                    .id("7.21")
                    .description("Uranium and thorium extraction")
                    .build();
            ActivityCode activityCode22 = ActivityCode.builder()
                    .id("8.11")
                    .description("Decorative and building stone extraction")
                    .build();
            ActivityCode activityCode23 = ActivityCode.builder()
                    .id("8.92")
                    .description("Peat extraction")
                    .build();
            ActivityCode activityCode24 = ActivityCode.builder()
                    .id("47.11")
                    .description("Retail trade in non-specialized food and beverage stores")
                    .build();
            ActivityCode activityCode25 = ActivityCode.builder()
                    .id("53.1")
                    .description("Postal activities")
                    .build();
            ActivityCode activityCode26 = ActivityCode.builder()
                    .id("53.2")
                    .description("Courier activities")
                    .build();
            ActivityCode activityCode27 = ActivityCode.builder()
                    .id("85.1")
                    .description("Preschool education")
                    .build();
            ActivityCode activityCode28 = ActivityCode.builder()
                    .id("85.2")
                    .description("Primary education")
                    .build();
            ActivityCode activityCode29 = ActivityCode.builder()
                    .id("86.21")
                    .description("General medical practice")
                    .build();
            ActivityCode activityCode30 = ActivityCode.builder()
                    .id("86.22")
                    .description("Specialist medical practice")
                    .build();
            ActivityCode activityCode31 = ActivityCode.builder()
                    .id("86.9")
                    .description("Other healthcare activities")
                    .build();
            ActivityCode activityCode32 = ActivityCode.builder()
                    .id("84.12")
                    .description("Regulation of economic activities")
                    .build();
            ActivityCode activityCode33 = ActivityCode.builder()
                    .id("90.01")
                    .description("Theater activities")
                    .build();
            ActivityCode activityCode34 = ActivityCode.builder()
                    .id("90.04")
                    .description("Botanical and zoological gardens")
                    .build();
            ActivityCode activityCode35 = ActivityCode.builder()
                    .id("93.11")
                    .description("Operation of sports facilities")
                    .build();
            ActivityCode activityCode36 = ActivityCode.builder()
                    .id("93.13")
                    .description("Gym activities")
                    .build();
            ActivityCode activityCode37 = ActivityCode.builder()
                    .id("93.19")
                    .description("Other sports activities")
                    .build();
            ActivityCode activityCode38 = ActivityCode.builder()
                    .id("26.11")
                    .description("Electronic components manufacturing")
                    .build();
            ActivityCode activityCode39 = ActivityCode.builder()
                    .id("27.12")
                    .description("Electrical panels and boards manufacturing")
                    .build();
            ActivityCode activityCode40 = ActivityCode.builder()
                    .id("29.1")
                    .description("Motor vehicle manufacturing")
                    .build();
            activityCodeRepository.saveAll(List.of(activityCode, activityCode2, activityCode3, activityCode4, activityCode5, activityCode6, activityCode7, activityCode8, activityCode9, activityCode10, activityCode11, activityCode12, activityCode13, activityCode14, activityCode15, activityCode16, activityCode17, activityCode18, activityCode19, activityCode20, activityCode21, activityCode22, activityCode23, activityCode24, activityCode25, activityCode26, activityCode27, activityCode28, activityCode29, activityCode30, activityCode31, activityCode32, activityCode33, activityCode34, activityCode35, activityCode36, activityCode37, activityCode38, activityCode39, activityCode40));

        }
        // CHATGPT CODE END 😊

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
                    .userId(6L)
                    .build();
            authTokenRepository.save(authToken);

            AuthToken authToken1 = AuthToken.builder()
                    .token("df7ff5f0-70bd-492c-9569-ac5f3fbda7xd")
                    .type("request-card")
                    .createdAt(1741631004271L)
                    .expiresAt(3000000000000L)
                    .userId(6L)
                    .build();
            authTokenRepository.save(authToken1);
        }

        if (actuaryLimitRepository.count() == 0) {
            Employee e2 = employeeRepository.findByEmail("zika.p@example.com").orElseThrow();
            ActuaryLimit al2 = new ActuaryLimit(null, new BigDecimal("10000.00"), new BigDecimal("2500.00"), false, e2);

            actuaryLimitRepository.save(al2);

        }

        if (authorizedPersonelRepository.count() == 0) {
            Company company1 = new Company();
            company1.setId(3L);
            AuthorizedPersonel auth1 = AuthorizedPersonel.builder()
                    .id(1L)
                    .address("Adresa Janka")
                    .company(company1)
                    .dateOfBirth(LocalDate.of(2000,1,1))
                    .gender("M")
                    .firstName("Janko")
                    .lastName("Stefanovic")
                    .email("email@email.com")
                    .phoneNumber("0691153492")
                    .build();

            authorizedPersonelRepository.save(auth1);
        }

    }
}

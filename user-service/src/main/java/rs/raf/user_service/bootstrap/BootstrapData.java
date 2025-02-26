package rs.raf.user_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.repository.PermissionRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Component
public class BootstrapData implements CommandLineRunner {
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final PermissionRepository permissionRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        if (permissionRepository.count() == 0) {
            Permission adminPermission = Permission.builder()
                    .name("admin")
                    .build();
            permissionRepository.saveAll(Set.of(adminPermission));
        }

        Set<Permission> permissions = new HashSet<>(permissionRepository.findAll());

        if (clientRepository.count() == 0) {
            Client client = Client.builder()
                    .firstName("Marko")
                    .lastName("Markovic")
                    .email("marko.m@example.com")
                    .phone("0611158275")
                    .address("Pozeska 56")
                    .birthDate(dateFormat.parse("1990-05-15"))
                    .gender("M")
                    .password(passwordEncoder.encode("markomarko"))
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
                    .build();

            clientRepository.saveAll(Set.of(client, client2));
        }

        if (employeeRepository.count() == 0) {
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
                    .permissions(permissions)
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
                    .build();

            employeeRepository.saveAll(Set.of(employee, employee2));
        }
    }
}

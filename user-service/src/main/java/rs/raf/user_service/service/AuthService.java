package rs.raf.user_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.raf.user_service.configuration.JwtTokenUtil;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.EmployeeRepository;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    public AuthService(PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil, ClientRepository clientRepository, EmployeeRepository employeeRepository) {
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
    }

    public String authenticateClient(String email, String password) {
        Client user = clientRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        return jwtTokenUtil.generateToken(user.getEmail(),permissions);
    }

    public String authenticateEmployee(String email, String password) {
        Employee user = employeeRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        return jwtTokenUtil.generateToken(user.getEmail(),permissions);
    }
}

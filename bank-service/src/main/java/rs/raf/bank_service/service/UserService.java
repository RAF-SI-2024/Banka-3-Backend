package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class UserService {
    @Value("${http://localhost:8081/api/v1}") // za sad hardcoded ovde
    private String USER_SERVICE_URL;


}

package rs.raf.bank_service.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import rs.raf.bank_service.BankServiceApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = BankServiceApplication.class)
public class CucumberSpringConfiguration {
}

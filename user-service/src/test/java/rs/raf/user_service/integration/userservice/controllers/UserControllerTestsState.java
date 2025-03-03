package rs.raf.user_service.integration.userservice.controllers;

import io.cucumber.spring.ScenarioScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@ScenarioScope
public class UserControllerTestsState {

    String jwtToken;

}

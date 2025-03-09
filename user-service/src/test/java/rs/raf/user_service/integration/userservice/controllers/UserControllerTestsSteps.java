package rs.raf.user_service.integration.userservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import rs.raf.user_service.controller.ClientController;
import rs.raf.user_service.integration.userservice.UserServiceTestsConfig;

public class UserControllerTestsSteps extends UserServiceTestsConfig {


    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserControllerTestsState userControllerTestsState = new UserControllerTestsState();
    @Autowired
    ClientController clientController;
    @Autowired
    ObjectMapper objectMapper;
    String adminToken;
    ResultActions resultActions;

    @Given("an admin user is logged in")
    public void anAdminUserIsLoggedIn() {
        /*try {
            ResultActions resultActions = mockMvc.perform(
                    post("/api/auth/login/employee")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE)
                            .content("{\"username\":\"petar.p@example.com\",\"password\":\"admin\"}")
            ).andExpect(status().isOk());

            MvcResult mvcResult = resultActions.andReturn();

            String loginResponse = mvcResult.getResponse().getContentAsString();
            LoginResponseDto loginResponseDto = objectMapper.readValue(loginResponse, LoginResponseDto.class);
            userControllerTestsState.setJwtToken(loginResponseDto.getToken());
        } catch (Exception e) {
            fail(e.getMessage());
        }*/
    }

    @When("I request the client list with page {int} and size {int}")
    public void iRequestTheClientListWithPageAndSize(int arg0, int arg1) {
    }

    @Then("I should receive a list of clients")
    public void iShouldReceiveAListOfClients() {
    }
}

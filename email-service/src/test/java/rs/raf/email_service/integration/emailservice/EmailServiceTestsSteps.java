package rs.raf.email_service.integration.emailservice;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import rs.raf.email_service.EmailListener;
import rs.raf.email_service.EmailRequestDto;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class EmailServiceTestsSteps extends EmailServiceTestsConfig {

    private GreenMail greenMail;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private EmailListener emailListener;

    @Before
    public void setup() {
        greenMail = new GreenMail(ServerSetupTest.SMTP)
                .withConfiguration(GreenMailConfiguration.aConfig().withUser("user", "admin"));
        greenMail.start();

        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;
        mailSenderImpl.setHost("localhost");
        mailSenderImpl.setPort(3025);
        mailSenderImpl.setUsername("user");
        mailSenderImpl.setPassword("admin");

        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.starttls.enable", "false");
        mailSenderImpl.setJavaMailProperties(mailProps);
    }

    @After
    public void tearDown() {
        greenMail.stop();
    }

    @Given("a reset password email request with token {string} and destination {string}")
    public void aResetPasswordEmailRequestWithTokenAndDestination(String arg0, String arg1) {
        EmailRequestDto dto = new EmailRequestDto();
        dto.setCode(arg0);
        dto.setDestination(arg1);

        try {
            emailListener.handleResetPassword(dto);
        } catch (MessagingException e) {
            fail(e.getMessage());
        }
    }

    @When("the email is sent")
    public void theEmailIsSent() {
    }

    @Then("the subject of the email should be {string}")
    public void theSubjectOfTheEmailShouldBe(String arg0) {
        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];

        try {
            assertEquals(arg0, message.getSubject());
        } catch (MessagingException e) {
            fail(e.getMessage());
        }
    }

    @And("the recipient of the email should be {string}")
    public void theRecipientOfTheEmailShouldBe(String arg0) {
        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];

        try {
            assertEquals(arg0, message.getAllRecipients()[0].toString());
        } catch (javax.mail.MessagingException e) {
            fail(e.getMessage());
        }
    }

    @And("the email content should contain {string}")
    public void theEmailContentShouldContain(String arg0) {
        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];

        String content = GreenMailUtil.getBody(message);
        assertTrue(content.contains(arg0));
    }

    @Given("a set password email request with token {string} and destination {string}")
    public void aSetPasswordEmailRequestWithTokenAndDestination(String arg0, String arg1) {
        EmailRequestDto dto = new EmailRequestDto();
        dto.setCode(arg0);
        dto.setDestination(arg1);

        try {
            emailListener.handleSetPassword(dto);
        } catch (MessagingException e) {
            fail(e.getMessage());
        }
    }

    @Given("an activate account email request with token {string} and destination {string}")
    public void anActivateAccountEmailRequestWithTokenAndDestination(String arg0, String arg1) {
        EmailRequestDto dto = new EmailRequestDto();
        dto.setCode(arg0);
        dto.setDestination(arg1);

        try {
            emailListener.handleActivateAccount(dto);
        } catch (MessagingException e) {
            fail(e.getMessage());
        }
    }


    @And("the email content should contain token {string}")
    public void theEmailContentShouldContainToken(String arg0) {
        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];

        String content = GreenMailUtil.getBody(message);
        assertTrue(content.contains(arg0));
    }
}

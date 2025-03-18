package rs.raf.bank_service.unit;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.client.UserClientFallbackFactory;
import rs.raf.bank_service.domain.dto.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UserClientFallbackFactoryTest {

    private final UserClientFallbackFactory fallbackFactory = new UserClientFallbackFactory();

    @Test
    void testGetClientById_NotFound() {
        FeignException cause = mock(FeignException.NotFound.class);
        UserClient fallback = fallbackFactory.create(cause);

        ClientDto result = fallback.getClientById(1L);

        assertNull(result);
    }

    @Test
    void testGetClientById_OtherError() {
        RuntimeException cause = new RuntimeException("Internal Error");
        UserClient fallback = fallbackFactory.create(cause);

        assertThrows(RuntimeException.class, () -> fallback.getClientById(1L));
    }

    @Test
    void testRequestCard() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertDoesNotThrow(() -> fallback.requestCard(new RequestCardDto()));
    }

    @Test
    void testCheckToken() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertDoesNotThrow(() -> fallback.checkToken(new CheckTokenDto()));
    }

    @Test
    void testGetCompanyById() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertNull(fallback.getCompanyById(1L));
    }

    @Test
    void testGetAuthorizedPersonnelByCompany() {
        UserClient fallback = fallbackFactory.create(new Exception());
        List<AuthorizedPersonelDto> list = fallback.getAuthorizedPersonnelByCompany(1L);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testCreateVerificationRequest() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertDoesNotThrow(() -> fallback.createVerificationRequest(new CreateVerificationRequestDto()));
    }

    @Test
    void testEmployeeLogin() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertNull(fallback.employeeLogin(new LoginRequestDto()));
    }

    @Test
    void testAddClient() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertNull(fallback.addClient(new CreateClientDto()));
    }

    @Test
    void testActivateUser() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertDoesNotThrow(() -> fallback.activateUser(new ActivationRequestDto()));
    }

    @Test
    void testGetAllClients() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertNull(fallback.getAllClients("John", "Doe", "john@example.com", 0, 10));
    }

    @Test
    void testClientLogin() {
        UserClient fallback = fallbackFactory.create(new Exception());
        assertNull(fallback.clientLogin(new LoginRequestDto()));
    }
}

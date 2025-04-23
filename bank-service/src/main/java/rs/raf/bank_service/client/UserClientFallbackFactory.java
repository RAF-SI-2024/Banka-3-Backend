package rs.raf.bank_service.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.*;

import java.util.Collections;
import java.util.List;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public ClientDto getClientById(Long id) {
                if (cause instanceof FeignException.NotFound) {
                    return null;
                }
                throw new RuntimeException(cause);
            }

            @Override
            public void checkToken(CheckTokenDto checkTokenDto) {
            }

            @Override
            public CompanyDto getCompanyById(Long id) {
                return null;
            }

            @Override
            public List<AuthorizedPersonelDto> getAuthorizedPersonnelByCompany(Long companyId) {
                return Collections.emptyList();
            }

            public void createVerificationRequest(CreateVerificationRequestDto request) {
                // Fallback logika - ne radi ništa, ali možemo logovati
                System.err.println("Fallback: createVerificationRequest failed.");
            }

            @Override
            public LoginResponseDto employeeLogin(LoginRequestDto request) {
                return null;
            }

            @Override
            public ClientDto addClient(CreateClientDto createClientDto) {
                return null;
            }

            @Override
            public void activateUser(ActivationRequestDto activationRequestDto) {

            }

            @Override
            public ResponseEntity<Page<ClientDto>> getAllClients(String firstName, String lastName, String email, int page, int size) {
                return null;
            }

            @Override
            public LoginResponseDto clientLogin(LoginRequestDto request) {
                return null;
            }

            @Override
            public AuthorizedPersonelDto getAuthorizedPersonnelById(Long id) {
                return null;
            }


            @Override
            public ActuaryLimitDto getAgentLimit(Long id) {
                return null;
            }

            @Override
            public ActuaryLimitDto updateUsedLimit(Long id, ChangeAgentLimitDto changeLimitRequest) {
                return null;
            }
        };
    }
}

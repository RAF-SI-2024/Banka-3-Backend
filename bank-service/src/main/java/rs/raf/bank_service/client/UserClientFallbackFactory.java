package rs.raf.bank_service.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
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
            public void requestCard(RequestCardDto requestCardDto) {
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

            @Override
            public void createVerificationRequest(VerificationRequestDto request) {
                // Fallback logika - ne radi ništa, ali možemo logovati
                System.err.println("Fallback: createVerificationRequest failed.");
            }

            @Override
            public boolean isVerificationApproved(Long targetId, String verificationCode) {
                // Ako ne može da se poveže, podrazumevano vratimo false (nije odobreno)
                System.err.println("Fallback: isVerificationApproved failed.");
                return false;
            }


        };
    }
}

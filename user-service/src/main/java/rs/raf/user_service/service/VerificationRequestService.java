package rs.raf.user_service.service;

import org.springframework.stereotype.Service;
import rs.raf.user_service.bankClient.BankClient;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.repository.VerificationRequestRepository;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VerificationRequestService {
    private final VerificationRequestRepository verificationRequestRepository;
    private final BankClient bankClient;

    public VerificationRequestService(VerificationRequestRepository verificationRequestRepository, BankClient bankClient) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.bankClient = bankClient;
    }

    public void createVerificationRequest(Long userId, String email, String code, Long transactionId) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(userId)
                .email(email)
                .code(code)
                .targetId(transactionId)
                .status(VerificationStatus.PENDING)
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();

        verificationRequestRepository.save(request);
    }

    public List<VerificationRequest> getActiveRequests(Long userId) {
        return verificationRequestRepository.findByUserIdAndStatus(userId, VerificationStatus.PENDING);
    }

    public boolean updateRequestStatus(Long requestId, VerificationStatus status) {
        return verificationRequestRepository.findById(requestId).map(request -> {
            request.setStatus(status);
            verificationRequestRepository.save(request);
            return true;
        }).orElse(false);
    }

    public boolean isVerificationApproved(Long targetId, String verificationCode) {
        return verificationRequestRepository.findByTargetIdAndStatus(targetId, VerificationStatus.APPROVED)
                .filter(request -> request.getCode().equals(verificationCode))
                .isPresent();
    }

    public boolean processApproval(Long requestId) {
        boolean updated = updateRequestStatus(requestId, VerificationStatus.APPROVED);

        if (updated) {
            // Dohvatamo zahtev da bismo izvukli ID ChangeLimitRequest entiteta
            VerificationRequest request = getRequestById(requestId);

            // Pozivamo bank-service da promeni limit
            bankClient.changeAccountLimit(request.getTargetId());

            return true;
        }
        return false;
    }

    public VerificationRequest getRequestById(Long requestId) {
        return verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Verification request not found"));
    }
}

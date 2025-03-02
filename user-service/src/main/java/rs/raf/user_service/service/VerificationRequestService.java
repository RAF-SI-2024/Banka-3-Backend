package rs.raf.user_service.service;

import org.springframework.stereotype.Service;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.repository.VerificationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VerificationRequestService {
    private final VerificationRequestRepository verificationRequestRepository;

    public VerificationRequestService(VerificationRequestRepository verificationRequestRepository) {
        this.verificationRequestRepository = verificationRequestRepository;
    }

    public void createVerificationRequest(Long userId, String email, Long transactionId) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(userId)
                .email(email)
                .transactionId(transactionId)
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
}

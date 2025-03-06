package rs.raf.user_service.service;

import org.springframework.stereotype.Service;
import rs.raf.user_service.bankClient.BankClient;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.enums.VerificationType;
import rs.raf.user_service.exceptions.VerificationNotFoundException;
import rs.raf.user_service.repository.VerificationRequestRepository;
import rs.raf.user_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VerificationRequestService {
    private final VerificationRequestRepository verificationRequestRepository;
    private final BankClient bankClient;
    private final JwtTokenUtil jwtTokenUtil;

    public VerificationRequestService(VerificationRequestRepository verificationRequestRepository, BankClient bankClient, JwtTokenUtil jwtTokenUtil) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.bankClient = bankClient;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public Long getTransactionIdByRequestId(Long requestId) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new VerificationNotFoundException(requestId));
        return request.getTargetId();  // Ovo je transactionId, koje je sačuvano u targetId
    }

    public void createVerificationRequest(Long userId, String email, Long transactionId) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(userId)
                .email(email)
                .targetId(transactionId)
                .status(VerificationStatus.PENDING)
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();

        verificationRequestRepository.save(request);
    }

    public void createPaymentVerificationRequest(Long userId, Long transactionId) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(userId)
                .targetId(transactionId)
                .verificationType(VerificationType.PAYMENT)  // Postavljanje tipa na PAYMENT
                .status(VerificationStatus.PENDING)  // Status PENDING dok čekamo verifikaciju
                .expirationTime(LocalDateTime.now().plusMinutes(5))  // Verifikacija je validna 5 minuta
                .build();

        verificationRequestRepository.save(request);  // Spremamo u bazu
    }

    public void createTransferVerificationRequest(Long userId, Long transactionId) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(userId)
                .targetId(transactionId)
                .verificationType(VerificationType.TRANSFER)  // Postavljanje tipa na TRANSFER
                .status(VerificationStatus.PENDING)  // Status PENDING dok čekamo verifikaciju
                .expirationTime(LocalDateTime.now().plusMinutes(5))  // Verifikacija je validna 5 minuta
                .build();

        verificationRequestRepository.save(request);  // Spremamo u bazu
    }

    public VerificationType getVerificationTypeByRequestId(Long requestId) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new VerificationNotFoundException(requestId));  // Ako ne nađe request, baci grešku

        // Vraćamo tip verifikacije (TRANSFER ili PAYMENT)
        return request.getVerificationType();
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


    public boolean processApproval(Long requestId, String authHeader) {

        Long clientIdFromToken = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);


        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Verification request not found"));


        if (!request.getUserId().equals(clientIdFromToken)) {
            throw new SecurityException("Unauthorized: You cannot approve this request");
        }


        request.setStatus(VerificationStatus.APPROVED);
        verificationRequestRepository.save(request);


        bankClient.changeAccountLimit(request.getTargetId());

        return true;
    }

    public VerificationRequest getRequestById(Long requestId) {
        return verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Verification request not found"));
    }

    public boolean denyVerificationRequest(Long requestId, String authHeader) {

        Long clientIdFromToken = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);


        VerificationRequest request = getRequestById(requestId);


        if (!request.getUserId().equals(clientIdFromToken)) {
            throw new SecurityException("You are not authorized to deny this request.");
        }


        return updateRequestStatus(requestId, VerificationStatus.DENIED);
    }
}

package rs.raf.user_service.service;

import org.springframework.stereotype.Service;
import rs.raf.user_service.client.BankClient;
import rs.raf.user_service.domain.dto.CreateVerificationRequestDto;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;
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

    public void createVerificationRequest(CreateVerificationRequestDto createVerificationRequestDto) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(createVerificationRequestDto.getUserId())
                .targetId(createVerificationRequestDto.getTargetId())
                .status(VerificationStatus.PENDING)
                .verificationType(createVerificationRequestDto.getVerificationType())
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();

        verificationRequestRepository.save(request);
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

        switch (request.getVerificationType()) {
            case CHANGE_LIMIT -> bankClient.changeAccountLimit(request.getTargetId());
            case PAYMENT -> bankClient.confirmPayment(request.getTargetId());
            case TRANSFER -> bankClient.confirmTransfer(request.getTargetId());
        }

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

        switch (request.getVerificationType()) {
        // @todo cancel payment/transfer, cancel limit change
        }

        return updateRequestStatus(requestId, VerificationStatus.DENIED);
    }
}

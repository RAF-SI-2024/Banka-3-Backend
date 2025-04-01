package rs.raf.user_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import rs.raf.user_service.client.BankClient;
import rs.raf.user_service.domain.dto.CardRequestDto;
import rs.raf.user_service.domain.dto.CreateVerificationRequestDto;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;
import rs.raf.user_service.exceptions.RejectNonPendingRequestException;
import rs.raf.user_service.exceptions.VerificationNotFoundException;
import rs.raf.user_service.repository.VerificationRequestRepository;
import rs.raf.user_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class VerificationRequestService {
    private final VerificationRequestRepository verificationRequestRepository;
    private final BankClient bankClient;
    private final JwtTokenUtil jwtTokenUtil;
    private ObjectMapper objectMapper;


    public void createVerificationRequest(CreateVerificationRequestDto createVerificationRequestDto) {
        VerificationRequest request = VerificationRequest.builder()
                .userId(createVerificationRequestDto.getUserId())
                .targetId(createVerificationRequestDto.getTargetId())
                .status(VerificationStatus.PENDING)
                .verificationType(createVerificationRequestDto.getVerificationType())
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .details(createVerificationRequestDto.getDetails())
                .build();

        verificationRequestRepository.save(request);
    }


    public List<VerificationRequest> getActiveRequests(Long userId) {
        return verificationRequestRepository.findActiveRequests(userId);
    }

    public List<VerificationRequest> getRequestHistory(Long userId) {
        return verificationRequestRepository.findInactiveRequests(userId);
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

        VerificationRequest request = verificationRequestRepository.findActiveRequest(requestId, clientIdFromToken)
                .orElseThrow(() -> new IllegalStateException("Verification request not found"));

        request.setStatus(VerificationStatus.APPROVED);
        verificationRequestRepository.save(request);

        switch (request.getVerificationType()) {
            case CHANGE_LIMIT -> bankClient.changeAccountLimit(request.getTargetId());//radi
            case PAYMENT -> bankClient.confirmPayment(request.getTargetId());//radi
            case TRANSFER -> bankClient.confirmTransfer(request.getTargetId());
            case CARD_REQUEST -> bankClient.approveCardRequest(request.getTargetId());//radi
        }

        return true;
    }


    public void denyVerificationRequest(Long requestId, String authHeader) {
        Long clientIdFromToken = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        VerificationRequest request = verificationRequestRepository.findActiveRequest(requestId, clientIdFromToken)
                .orElseThrow(() -> new VerificationNotFoundException(requestId));

        if(!request.getStatus().equals(VerificationStatus.PENDING))
            throw new RejectNonPendingRequestException();

        request.setStatus(VerificationStatus.DENIED);
        verificationRequestRepository.save(request);

        switch (request.getVerificationType()) {
            case CHANGE_LIMIT -> bankClient.rejectChangeAccountLimit(request.getTargetId());
            case PAYMENT -> bankClient.rejectConfirmPayment(request.getTargetId());
            case TRANSFER -> bankClient.rejectConfirmTransfer(request.getTargetId());
            case CARD_REQUEST -> bankClient.rejectApproveCardRequest(request.getTargetId());
        }
    }

    public boolean calledFromMobile(String userAgent) {
        return userAgent != null && userAgent.equals("MobileApp/1.0");
    }
}

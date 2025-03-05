package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.CreditRequestDto;
import rs.raf.bank_service.domain.entity.CreditRequest;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;
import rs.raf.bank_service.repository.CreditRequestRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CreditRequestService {
    private final CreditRequestRepository creditRequestRepository;

    public CreditRequestService(CreditRequestRepository creditRequestRepository) {
        this.creditRequestRepository = creditRequestRepository;
    }

    public CreditRequest submitCreditRequest(CreditRequest creditRequest) {
        creditRequest.setApproval(CreditRequestApproval.PENDING);
        return creditRequestRepository.save(creditRequest);
    }

    public List<CreditRequestDto> getRequestsByStatus(CreditRequestApproval approval) {
        return creditRequestRepository.findByApproval(approval).stream().map(
                creditRequest -> new CreditRequestDto(creditRequest.getAccountNumber(), creditRequest.getCreditType(),
                        creditRequest.getAmount(), creditRequest.getCurrency(), creditRequest.getPurpose(), creditRequest.getMonthlySalary(), creditRequest.getEmploymentStatus(),
                        creditRequest.getEmploymentPeriod(), creditRequest.getRepaymentPeriod(), creditRequest.getBranch(), creditRequest.getPhoneNumber(), creditRequest.getApproval(), creditRequest.getBankAccountNumber())
        ).collect(Collectors.toList());
    }

    public CreditRequest acceptRequest(Long requestId) {
        CreditRequest request = creditRequestRepository.findById(requestId).orElseThrow();
        request.setApproval(CreditRequestApproval.ACCEPTED);
        return creditRequestRepository.save(request);
    }

    public CreditRequest denyRequest(Long requestId) {
        CreditRequest request = creditRequestRepository.findById(requestId).orElseThrow();
        request.setApproval(CreditRequestApproval.DENIED);
        return creditRequestRepository.save(request);
    }

    public CreditRequest pendingRequest(Long requestId) {
        CreditRequest request = creditRequestRepository.findById(requestId).orElseThrow();
        request.setApproval(CreditRequestApproval.PENDING);
        return creditRequestRepository.save(request);
    }
}

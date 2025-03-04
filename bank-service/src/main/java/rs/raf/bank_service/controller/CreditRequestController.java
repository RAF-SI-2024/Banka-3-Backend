package rs.raf.bank_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CreditRequestDTO;
import rs.raf.bank_service.domain.entity.CreditRequest;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;
import rs.raf.bank_service.service.CreditRequestService;

import java.util.List;

@RestController
@RequestMapping("/credit-requests")
public class CreditRequestController {
    private final CreditRequestService creditRequestService;

    public CreditRequestController(CreditRequestService creditRequestService) {
        this.creditRequestService = creditRequestService;
    }

    @PostMapping
    public ResponseEntity<CreditRequest> submitRequest(@RequestBody CreditRequest creditRequest) {
        return ResponseEntity.ok(creditRequestService.submitCreditRequest(creditRequest));
    }

    @GetMapping("/status/{approval}")
    public ResponseEntity<List<CreditRequestDTO>> getPendingRequests(@PathVariable CreditRequestApproval approval) {
        return ResponseEntity.ok(creditRequestService.getRequestsByStatus(approval));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<CreditRequest> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.ok(creditRequestService.acceptRequest(id));
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<CreditRequest> denyRequest(@PathVariable Long id) {
        return ResponseEntity.ok(creditRequestService.denyRequest(id));
    }

    @PostMapping("/{id}/pending")
    public ResponseEntity<CreditRequest> pendingRequest(@PathVariable Long id) {
        return ResponseEntity.ok(creditRequestService.pendingRequest(id));
    }
}

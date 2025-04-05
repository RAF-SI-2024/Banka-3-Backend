package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.service.OtcService;

@RestController
@RequestMapping("/api/otc-options")
@RequiredArgsConstructor
public class OtcController {

    private final OtcService otcService;

    @Operation(
            summary = "Exercise OTC option",
            description = "Allows a buyer to exercise an OTC option if valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTC option exercised successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid option or business rules violated"),
            @ApiResponse(responseCode = "500", description = "Unexpected error occurred during option execution")
    })
    @PostMapping("/{optionId}/exercise")
    public ResponseEntity<Void> exerciseOtcOption(@PathVariable Long optionId, @RequestHeader("userId") Long userId) {
        otcService.exerciseOption(optionId, userId);
        return ResponseEntity.ok().build();
    }
}


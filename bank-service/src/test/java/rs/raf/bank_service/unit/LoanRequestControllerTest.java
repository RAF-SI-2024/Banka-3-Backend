package rs.raf.bank_service.unit;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.controller.LoanRequestController;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.service.LoanRequestService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LoanRequestControllerTest {


    private MockMvc mockMvc;

    private LoanRequestService loanRequestService;
    private LoanRequestController loanRequestController;

    @BeforeEach
    void setUp() {
        loanRequestService = Mockito.mock(LoanRequestService.class);
        loanRequestController = new LoanRequestController(loanRequestService);
        mockMvc = MockMvcBuilders.standaloneSetup(loanRequestController).build();
    }

    @Test
    void getLoanRequestsByStatus() throws Exception {
        LoanRequestDto loanRequestDto = new LoanRequestDto();
        when(loanRequestService.getLoanRequestsByStatus(LoanRequestStatus.PENDING))
                .thenReturn(Collections.singletonList(loanRequestDto));

        mockMvc.perform(get("/loan-requests/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createLoanRequest() throws Exception {
        LoanRequestDto loanRequestDto = new LoanRequestDto();
        when(loanRequestService.saveLoanRequest(Mockito.any(LoanRequestDto.class)))
                .thenReturn(loanRequestDto);

        mockMvc.perform(post("/loan-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"amount\": 10000, \"repaymentPeriod\": 12 }"))
                .andExpect(status().isOk());
    }
}




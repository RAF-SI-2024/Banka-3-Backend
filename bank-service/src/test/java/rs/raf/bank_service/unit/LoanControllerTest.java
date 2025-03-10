package rs.raf.bank_service.unit;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.controller.LoanController;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.service.LoanService;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class LoanControllerTest {


    private MockMvc mockMvc;

    private LoanService loanService;
    private LoanController loanController;

    @BeforeEach
    void setUp() {
        loanService = Mockito.mock(LoanService.class);
        loanController = new LoanController(loanService);
        mockMvc = MockMvcBuilders.standaloneSetup(loanController).build();
    }

    @Test
    void approveLoan() throws Exception {
        Loan loan = new Loan();
        when(loanService.approveLoan(1L)).thenReturn(loan);

        mockMvc.perform(post("/api/loan/approve/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void rejectLoan() throws Exception {
        doNothing().when(loanService).rejectLoan(1L);

        mockMvc.perform(post("/api/loan/reject/1"))
                .andExpect(status().isOk());
    }

     
}

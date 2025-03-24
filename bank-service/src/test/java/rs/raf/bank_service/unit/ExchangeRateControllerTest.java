package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.catalina.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.controller.ExchangeRateController;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExchangeRateController.class, excludeAutoConfiguration = SecurityConfig.class)
public class ExchangeRateControllerTest {

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    private ConvertDto dummyConvertDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dummyConvertDto = new ConvertDto("EUR", "RSD", BigDecimal.valueOf(100));

        when(jwtTokenUtil.getUserIdFromAuthHeader(anyString())).thenReturn(123L); // Mock JWT behavior
    }

    @Test
    @WithMockUser
        // Simulating an authenticated user
    void testGetExchangeRates_Success() throws Exception {
        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser
    void testGetExchangeRates_Failure() throws Exception {
        doThrow(new RuntimeException("Exchange rate retrieval failed"))
                .when(exchangeRateService).getExchangeRates();

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isInternalServerError());
    }


    @Test
    @WithMockUser
    void testConvert_Success() throws Exception {
        when(exchangeRateService.convert(dummyConvertDto)).thenReturn(BigDecimal.valueOf(11700));

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dummyConvertDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testConvert_NotFound() throws Exception {
        // Mock exchangeRateService to throw CurrencyNotFoundException
        doThrow(new CurrencyNotFoundException("EUR"))
                .when(exchangeRateService).convert(any(ConvertDto.class));

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dummyConvertDto)))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser
    void testConvert_ExchangeRateNotFound() throws Exception {
        // Ensure the service throws ExchangeRateNotFoundException
        doThrow(new ExchangeRateNotFoundException("EUR", "RSD"))
                .when(exchangeRateService).convert(any(ConvertDto.class));

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dummyConvertDto)))
                .andExpect(status().isNotFound()); // Expect 404 response
    }


    @Test
    @WithMockUser
    void testConvert_Failure() throws Exception {
        // Mock exchangeRateService to throw a RuntimeException
        doThrow(new RuntimeException("Conversion failed"))
                .when(exchangeRateService).convert(any(ConvertDto.class));

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dummyConvertDto)))
                .andExpect(status().isInternalServerError());
    }

}

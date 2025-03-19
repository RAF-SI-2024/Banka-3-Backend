package rs.raf.stock_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.stock_service.controller.StocksController;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.dto.StockSearchDto;
import rs.raf.stock_service.service.StocksService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class StocksControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    private StocksService stockService;
    @InjectMocks
    private StocksController stocksController;

    @BeforeEach
    public void setUp() {
        stocksController = new StocksController(stockService);
        mockMvc = MockMvcBuilders.standaloneSetup(stocksController).build();
    }

    @Test
    public void testSearchByTicker_Success() throws Exception {
        StockSearchDto dto1 = new StockSearchDto();
        dto1.setTicker("AAPL");
        dto1.setName("Apple Inc.");
        dto1.setRegion("US");
        dto1.setMatchScore("1.0000");
        List<StockSearchDto> list = Arrays.asList(dto1);
        when(stockService.searchByTicker("Apple")).thenReturn(list);

        mockMvc.perform(get("/api/stocks/search")
                        .param("keywords", "Apple")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"));
    }

    @Test
    public void testGetStock_Success() throws Exception {
        StockDto dto = new StockDto();
        dto.setTicker("AAPL");
        dto.setName("Apple Inc.");
        dto.setPrice(new BigDecimal("150.00"));
        when(stockService.getStockData("AAPL")).thenReturn(dto);

        mockMvc.perform(get("/api/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."));
    }

    @Test
    public void testGetAllStocks_Success() throws Exception {
        StockDto dto1 = new StockDto();
        dto1.setTicker("AAPL");
        StockDto dto2 = new StockDto();
        dto2.setTicker("MSFT");
        List<StockDto> list = Arrays.asList(dto1, dto2);
        Page<StockDto> page = new PageImpl<>(list, PageRequest.of(0, 10), list.size());

        when(stockService.getStocksList(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/stocks/all")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.content[1].ticker").value("MSFT"));
    }
}

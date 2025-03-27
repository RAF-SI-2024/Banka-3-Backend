package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.FuturesContractDto;
import rs.raf.stock_service.domain.entity.FuturesContract;
import rs.raf.stock_service.repository.FuturesRepository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class FuturesService {

    private final FuturesRepository futuresRepository;

    public FuturesContractDto getFuturesContractByTicker(String ticker) {
        FuturesContract fc = futuresRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Futures contract not found for ticker: " + ticker));

        FuturesContractDto dto = new FuturesContractDto();
        dto.setTicker(fc.getTicker());
        dto.setContractSize(fc.getContractSize());
        dto.setContractUnit(fc.getContractUnit());
        dto.setMaintenanceMargin(fc.getMaintenanceMargin());
        dto.setSettlementDate(fc.getSettlementDate());
        dto.setPrice(fc.getPrice());

        return dto;
    }

    public List<FuturesContractDto> getFuturesContracts() {
        List<FuturesContractDto> dtos = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/future_data.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length < 5) continue;

                String contractName = fields[0].trim();
                int contractSize = Integer.parseInt(fields[1].trim());
                String contractUnit = fields[2].trim();
                BigDecimal maintenanceMargin = new BigDecimal(fields[3].trim());

                // margin / (contractSize * 0.10)
                BigDecimal price = maintenanceMargin
                        .divide(new BigDecimal(contractSize), 10, BigDecimal.ROUND_HALF_UP)
                        .divide(new BigDecimal("0.10"), 2, BigDecimal.ROUND_HALF_UP);

                // Generate random future settlement date (30 to 210 days from now)
                LocalDate settlementDate = LocalDate.now().plusDays((int) (Math.random() * 180) + 30);

                FuturesContractDto dto = new FuturesContractDto();
                dto.setTicker(contractName.toUpperCase().replaceAll("\\s+", "_"));
                dto.setContractSize(contractSize);
                dto.setContractUnit(contractUnit);
                dto.setMaintenanceMargin(maintenanceMargin);
                dto.setPrice(price);
                dto.setSettlementDate(settlementDate);

                dtos.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading futures CSV file: " + e.getMessage(), e);
        }
        return dtos;
    }
}

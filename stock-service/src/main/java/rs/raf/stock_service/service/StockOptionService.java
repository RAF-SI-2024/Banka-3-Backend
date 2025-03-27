package rs.raf.stock_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.StockOptionDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.mapper.StockOptionMapper;
import rs.raf.stock_service.repository.OptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockOptionService {

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private StockOptionMapper stockOptionMapper;


    public List<StockOptionDto> getStockOptionsByDate(Long stockId, LocalDate settlementDate) {
        List<Option> options = optionRepository.findByUnderlyingStockIdAndSettlementDate(stockId, settlementDate);
        return options.stream()
                .map(stockOptionMapper::toDto)
                .collect(Collectors.toList());
    }
}

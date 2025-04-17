package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class TaxService {

    private final UserClient userClient;
    private final BankClient bankClient;
    private final PortfolioService portfolioService;
    private final OrderRepository orderRepository;

    public List<UserTaxDto> getTaxes(String name, String surname, String role) {
        List<UserTaxDto> userTaxDtos = userClient.getAgentsAndClients(name, surname, role);
        for (UserTaxDto userTaxDto : userTaxDtos) {
            TaxGetResponseDto taxForUser = portfolioService.getUserTaxes(userTaxDto.getId());
            userTaxDto.setUnpaidTaxThisMonth(bankClient.convert(new ConvertDto("USD", "RSD", taxForUser.getUnpaidForThisMonth())));
            userTaxDto.setPaidTaxThisYear(bankClient.convert(new ConvertDto("USD", "RSD", taxForUser.getPaidForThisYear())));
        }
        return userTaxDtos;
    }

    //@Scheduled(cron = "0 0 0 * * *")
    public boolean processTaxes() {
        boolean success = true;
        for (Order order : orderRepository.findAll()) {
            if (order.getTaxAmount() != null && order.getTaxStatus().equals(TaxStatus.PENDING)) {
                AccountDetailsDto accountDetailsDto = bankClient.getAccountDetails(order.getAccountNumber());
                BigDecimal taxAmount;
                if (!accountDetailsDto.getCurrencyCode().equals("USD")) {
                    ConvertDto convertDto = new ConvertDto("USD", accountDetailsDto.getCurrencyCode(), order.getTaxAmount());
                    taxAmount = bankClient.convert(convertDto);
                } else
                    taxAmount = order.getTaxAmount();

                if (accountDetailsDto.getBalance().compareTo(taxAmount) >= 0) {
                    TaxDto taxDto = new TaxDto();
                    taxDto.setAmount(taxAmount);
                    taxDto.setClientId(order.getUserId());
                    taxDto.setSenderAccountNumber(order.getAccountNumber());
                    bankClient.handleTax(taxDto);
                    //                     order.setTaxStatus(TaxStatus.PAID);
                    //                    orderRepository.save(order);
                } else {
                    success = false;
                }
            }
        }
        return success;
    }
}

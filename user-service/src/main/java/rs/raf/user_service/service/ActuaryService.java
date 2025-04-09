package rs.raf.user_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rs.raf.user_service.client.StockClient;
import rs.raf.user_service.domain.dto.*;
import rs.raf.user_service.domain.entity.ActuaryLimit;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.mapper.ActuaryMapper;
import rs.raf.user_service.domain.mapper.EmployeeMapper;
import rs.raf.user_service.exceptions.ActuaryLimitNotFoundException;
import rs.raf.user_service.exceptions.EmployeeNotFoundException;
import rs.raf.user_service.exceptions.UserNotAgentException;
import rs.raf.user_service.repository.ActuaryLimitRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.specification.EmployeeSearchSpecification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ActuaryService {

    private final ActuaryLimitRepository actuaryLimitRepository;
    private final EmployeeRepository employeeRepository;
    private final StockClient stockClient;

    public Page<EmployeeDto> findAgents(String firstName, String lastName, String email, String position, Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSearchSpecification.startsWithFirstName(firstName))
                .and(EmployeeSearchSpecification.startsWithLastName(lastName))
                .and(EmployeeSearchSpecification.startsWithEmail(email))
                .and(EmployeeSearchSpecification.startsWithPosition(position))
                .and(EmployeeSearchSpecification.hasRole("AGENT"));

        return employeeRepository.findAll(spec, pageable)
                .map(EmployeeMapper::toDto);


    }

    public Page<ActuaryDto> findActuaries(Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSearchSpecification.hasRole("AGENT")
                .or(EmployeeSearchSpecification.hasRole("SUPERVISOR")));

        Page<ActuaryDto> actuaryDtoPage = employeeRepository.findAll(spec, pageable).map(ActuaryMapper::toActuaryDto);

        List<OrderDto> orderDtos = stockClient.getAll();

        for (OrderDto orderDto : orderDtos) {
            ActuaryDto actuaryDto = actuaryDtoPage.getContent().stream()
                    .filter(actuary -> actuary.getId().equals(orderDto.getUserId()))
                    .findFirst()
                    .orElse(null);
            if (actuaryDto == null) {
                System.out.println("Greska actuaryDto je null");
                continue;
            }
            actuaryDto.setProfit(actuaryDto.getProfit().add(orderDto.getProfit()));
        }
        return actuaryDtoPage;
    }

    public void changeAgentLimit(Long employeeId, BigDecimal newLimit) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        if (!Objects.equals(employee.getRole().getName(), "AGENT"))
            throw new UserNotAgentException(employeeId);
        ActuaryLimit actuaryLimit = actuaryLimitRepository.findByEmployeeId(employeeId).orElseThrow(() -> new ActuaryLimitNotFoundException(employeeId));
        actuaryLimit.setLimitAmount(newLimit);
        actuaryLimitRepository.save(actuaryLimit);
    }

    public void resetDailyLimit(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        if (!Objects.equals(employee.getRole().getName(), "AGENT"))
            throw new UserNotAgentException(employeeId);

        ActuaryLimit actuaryLimit = actuaryLimitRepository.findByEmployeeId(employeeId).orElseThrow(() -> new ActuaryLimitNotFoundException(employeeId));
        actuaryLimit.setUsedLimit(BigDecimal.ZERO);
        actuaryLimitRepository.save(actuaryLimit);
    }

    public void setApproval(Long employeeId, boolean value) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        if (!Objects.equals(employee.getRole().getName(), "AGENT"))
            throw new UserNotAgentException(employeeId);

        ActuaryLimit actuaryLimit = actuaryLimitRepository.findByEmployeeId(employeeId).orElseThrow(() -> new ActuaryLimitNotFoundException(employeeId));
        actuaryLimit.setNeedsApproval(value);
        actuaryLimitRepository.save(actuaryLimit);
    }

    public ActuaryLimitDto getAgentLimit(Long id) {
        ActuaryLimit actuaryLimit = actuaryLimitRepository.findByEmployeeId(id).orElseThrow(() -> new ActuaryLimitNotFoundException(id));
        ActuaryLimitDto actuaryLimitDto = new ActuaryLimitDto();
        actuaryLimitDto.setLimitAmount(actuaryLimit.getLimitAmount());
        actuaryLimitDto.setUsedLimit(actuaryLimit.getUsedLimit());
        actuaryLimitDto.setNeedsApproval(actuaryLimit.isNeedsApproval());
        return actuaryLimitDto;
    }

    //Na svakih 15 sekundi
    //@Scheduled(cron = "*/15 * * * * *")

    //U ponoc
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyLimits() {
        List<ActuaryLimit> limits = actuaryLimitRepository.findAll();
        for (ActuaryLimit limit : limits) {
            limit.setUsedLimit(BigDecimal.ZERO);
        }
        System.out.println("Daily used limits have been reset.");
        actuaryLimitRepository.saveAll(limits);
    }

}

package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.domain.entity.Payee;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.exceptions.DuplicatePayeeException;
import rs.raf.bank_service.mapper.PayeeMapper;
import rs.raf.bank_service.repository.PayeeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayeeService {

    private final PayeeRepository repository;
    private final PayeeMapper mapper;

    public List<PayeeDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public PayeeDto create(PayeeDto dto) {
        if (repository.findByAccountNumber(dto.getAccountNumber()).isPresent()) {
            throw new DuplicatePayeeException(dto.getAccountNumber());
        }

        Payee payee = mapper.toEntity(dto);
        return mapper.toDto(repository.save(payee));
    }

    public PayeeDto update(Long id, PayeeDto dto) {
        Payee payee = repository.findById(id)
                .orElseThrow(() -> new PayeeNotFoundException(id));

        payee.setName(dto.getName());
        payee.setAccountNumber(dto.getAccountNumber());

        return mapper.toDto(repository.save(payee));
    }

    public void delete(Long id) {
        Payee payee = repository.findById(id)
                .orElseThrow(() -> new PayeeNotFoundException(id));
        repository.delete(payee);
    }
}

package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.domain.entity.Payee;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.DuplicatePayeeException;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.domain.mapper.PayeeMapper;
import rs.raf.bank_service.repository.PayeeRepository;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayeeService {

    private final PayeeRepository repository;
    private final PayeeMapper mapper;
    private final JwtTokenUtil jwtTokenUtil;


    public Long getClientIdFromToken(String token) {
        token = token.replace("Bearer ", "");
        if (!jwtTokenUtil.validateToken(token)) {
            throw new SecurityException("Invalid token");
        }
        return Long.valueOf(jwtTokenUtil.extractUserId(token));
    }

    public List<PayeeDto> getByClientId(Long clientId) {
        List<Payee> payees = repository.findByClientId(clientId);
        return payees.stream()
                .map(mapper::toDto)
                .toList();
    }

    public PayeeDto create(PayeeDto dto, Long clientId) {

        Optional<Payee> existingPayee = repository.findByAccountNumberAndClientId(dto.getAccountNumber(), clientId); //ispravljena metoda u PayeeRepository
        if (existingPayee.isPresent()) {
            throw new DuplicatePayeeException(dto.getAccountNumber()); //baca ovu gresku samo ako se poklopi clientId i accountNumber, u ostalim slucajevima moze
        }

        Payee payee = mapper.toEntity(dto);
        payee.setClientId(clientId);
        return mapper.toDto(repository.save(payee));
    }

    public PayeeDto update(Long id, PayeeDto dto, Long clientId) {
        Payee payee = repository.findById(id)
                .orElseThrow(() -> new PayeeNotFoundException(id));


        if (!payee.getClientId().equals(clientId)) {
            throw new ClientNotFoundException(clientId);
        }

        payee.setName(dto.getName());
        payee.setAccountNumber(dto.getAccountNumber());

        return mapper.toDto(repository.save(payee));
    }

    public void delete(Long id, Long clientId) {

        Payee payee = repository.findById(id)
                .orElseThrow(() -> new PayeeNotFoundException(id));


        if (!payee.getClientId().equals(clientId)) {
            throw new ClientNotFoundException(clientId);
        }

        repository.delete(payee);
    }
}

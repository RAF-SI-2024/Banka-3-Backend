package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.mappers.InstallmentMapper;
import rs.raf.bank_service.repository.InstallmentRepository;

import java.util.List;

@Service
public class InstallmentService {
    private final InstallmentRepository installmentRepository;
    private final InstallmentMapper installmentMapper;

    public InstallmentService(InstallmentRepository installmentRepository, InstallmentMapper installmentMapper) {
        this.installmentRepository = installmentRepository;
        this.installmentMapper = installmentMapper;

    }

    public List<InstallmentDto> getInstallmentsByLoanId(Long loanId) {
        return installmentMapper.toDtoList(installmentRepository.findByLoanId(loanId));
    }
}
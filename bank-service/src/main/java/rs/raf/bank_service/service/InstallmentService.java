package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.InstallmentCreateDto;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.exceptions.LoanNotFoundException;
import rs.raf.bank_service.mappers.InstallmentMapper;
import rs.raf.bank_service.repository.InstallmentRepository;
import rs.raf.bank_service.repository.LoanRepository;

import java.util.List;

@Service
public class InstallmentService {
    private final InstallmentRepository installmentRepository;
    private final InstallmentMapper installmentMapper;

    private final LoanRepository loanRepository;

    public InstallmentService(InstallmentRepository installmentRepository, InstallmentMapper installmentMapper, LoanRepository loanRepository) {
        this.installmentRepository = installmentRepository;
        this.installmentMapper = installmentMapper;
        this.loanRepository = loanRepository;
    }

    public List<InstallmentDto> getInstallmentsByLoanId(Long loanId) {
        return installmentMapper.toDtoList(installmentRepository.findByLoanId(loanId));
    }

    public InstallmentCreateDto createInstalment(InstallmentCreateDto installmentDto) {
        Installment installment = installmentMapper.toEntity(installmentDto);
        installment.setLoan(loanRepository.findById(installmentDto.getLoanId()).orElseThrow(() -> new LoanNotFoundException(installmentDto.getLoanId())));
        installmentRepository.save(installment);
        return installmentDto;
    }
}
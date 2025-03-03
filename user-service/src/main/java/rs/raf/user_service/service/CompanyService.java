package rs.raf.user_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.CreateCompanyDto;
import rs.raf.user_service.entity.ActivityCode;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Company;
import rs.raf.user_service.exceptions.CompanyRegNumExistsException;
import rs.raf.user_service.exceptions.TaxIdAlreadyExistsException;
import rs.raf.user_service.repository.ActivityCodeRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.CompanyRepository;

import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final ActivityCodeRepository activityCodeRepository;

    public void createCompany(CreateCompanyDto createCompanyDto){
        Client client = clientRepository.findById(createCompanyDto.getMajorityOwner()).orElse(null);
        if(client == null)
            throw new NoSuchElementException("Owner not found with ID: " + createCompanyDto.getMajorityOwner());

        ActivityCode activityCode = activityCodeRepository.findById(createCompanyDto.getActivityCode()).orElse(null);
        if(activityCode == null)
            throw new NoSuchElementException("Activity code not found with ID: " + createCompanyDto.getActivityCode());
        if(companyRepository.findByRegistrationNumber(createCompanyDto.getRegistrationNumber()).isPresent())
            throw new CompanyRegNumExistsException(createCompanyDto.getRegistrationNumber());
        if(companyRepository.findByTaxId(createCompanyDto.getTaxId()).isPresent())
            throw new TaxIdAlreadyExistsException(createCompanyDto.getTaxId());
        Company company = new Company();
        company.setName(createCompanyDto.getName());
        company.setRegistrationNumber(createCompanyDto.getRegistrationNumber());
        company.setTaxId(createCompanyDto.getTaxId());
        company.setActivityCode(activityCode.getId());
        company.setAddress(createCompanyDto.getAddress());
        company.setMajorityOwner(client);

        companyRepository.save(company);
    }
}

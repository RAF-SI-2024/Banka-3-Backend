package rs.raf.user_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.AuthorizedPersonelDto;
import rs.raf.user_service.dto.CreateAuthorizedPersonelDto;
import rs.raf.user_service.entity.AuthorizedPersonel;
import rs.raf.user_service.entity.Company;
import rs.raf.user_service.mapper.AuthorizedPersonelMapper;
import rs.raf.user_service.repository.AuthorizedPersonelRepository;
import rs.raf.user_service.repository.CompanyRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthorizedPersonelService {

    private final AuthorizedPersonelRepository authorizedPersonelRepository;
    private final CompanyRepository companyRepository;
    private final AuthorizedPersonelMapper authorizedPersonelMapper;

    /**
     * Create a new AuthorizedPersonel
     * 
     * @param createAuthorizedPersonelDto DTO containing the data for the new
     *                                    AuthorizedPersonel
     * @return DTO of the created AuthorizedPersonel
     */
    public AuthorizedPersonelDto createAuthorizedPersonel(CreateAuthorizedPersonelDto createAuthorizedPersonelDto) {
        Company company = companyRepository.findById(createAuthorizedPersonelDto.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Company not found with id: " + createAuthorizedPersonelDto.getCompanyId()));

        AuthorizedPersonel authorizedPersonel = authorizedPersonelMapper.toEntity(createAuthorizedPersonelDto, company);
        authorizedPersonel = authorizedPersonelRepository.save(authorizedPersonel);

        return authorizedPersonelMapper.toDto(authorizedPersonel);
    }

    /**
     * Get all AuthorizedPersonel for a company
     * 
     * @param companyId ID of the company
     * @return List of AuthorizedPersonelDto
     */
    public List<AuthorizedPersonelDto> getAuthorizedPersonelByCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + companyId));

        List<AuthorizedPersonel> authorizedPersonelList = authorizedPersonelRepository.findByCompany(company);
        return authorizedPersonelList.stream()
                .map(authorizedPersonelMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get an AuthorizedPersonel by ID
     * 
     * @param id ID of the AuthorizedPersonel
     * @return DTO of the AuthorizedPersonel
     */
    public AuthorizedPersonelDto getAuthorizedPersonelById(Long id) {
        AuthorizedPersonel authorizedPersonel = authorizedPersonelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Authorized personnel not found with id: " + id));

        return authorizedPersonelMapper.toDto(authorizedPersonel);
    }

    /**
     * Update an AuthorizedPersonel
     * 
     * @param id        ID of the AuthorizedPersonel to update
     * @param updateDto DTO containing the updated data
     * @return DTO of the updated AuthorizedPersonel
     */
    public AuthorizedPersonelDto updateAuthorizedPersonel(Long id, CreateAuthorizedPersonelDto updateDto) {
        AuthorizedPersonel authorizedPersonel = authorizedPersonelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Authorized personnel not found with id: " + id));

        Company company = companyRepository.findById(updateDto.getCompanyId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Company not found with id: " + updateDto.getCompanyId()));

        authorizedPersonel.setFirstName(updateDto.getFirstName());
        authorizedPersonel.setLastName(updateDto.getLastName());
        authorizedPersonel.setDateOfBirth(updateDto.getDateOfBirth());
        authorizedPersonel.setGender(updateDto.getGender());
        authorizedPersonel.setEmail(updateDto.getEmail());
        authorizedPersonel.setPhoneNumber(updateDto.getPhoneNumber());
        authorizedPersonel.setAddress(updateDto.getAddress());
        authorizedPersonel.setCompany(company);

        authorizedPersonel = authorizedPersonelRepository.save(authorizedPersonel);
        return authorizedPersonelMapper.toDto(authorizedPersonel);
    }

    /**
     * Delete an AuthorizedPersonel
     * 
     * @param id ID of the AuthorizedPersonel to delete
     */
    public void deleteAuthorizedPersonel(Long id) {
        if (!authorizedPersonelRepository.existsById(id)) {
            throw new EntityNotFoundException("Authorized personnel not found with id: " + id);
        }
        authorizedPersonelRepository.deleteById(id);
    }

    /**
     * Check if a client is the majority owner of a company
     * 
     * @param companyId ID of the company
     * @param clientId  ID of the client
     * @return true if the client is the majority owner of the company, false
     *         otherwise
     */
    public boolean isClientMajorityOwnerOfCompany(Long companyId, Long clientId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + companyId));

        // Check if the client is the majority owner of the company
        return company.getMajorityOwner() != null && company.getMajorityOwner().getId().equals(clientId);
    }
}
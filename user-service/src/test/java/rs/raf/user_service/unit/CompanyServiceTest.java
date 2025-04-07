package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.user_service.domain.dto.CompanyDto;
import rs.raf.user_service.domain.dto.CreateCompanyDto;
import rs.raf.user_service.domain.entity.ActivityCode;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.domain.entity.Company;
import rs.raf.user_service.exceptions.CompanyNotFoundException;
import rs.raf.user_service.exceptions.CompanyRegNumExistsException;
import rs.raf.user_service.exceptions.TaxIdAlreadyExistsException;
import rs.raf.user_service.repository.ActivityCodeRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.CompanyRepository;
import rs.raf.user_service.service.CompanyService;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ActivityCodeRepository activityCodeRepository;

    @InjectMocks
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -------------------------------------------------------------------
    // createCompany(...)
    // -------------------------------------------------------------------
    @Test
    void testCreateCompany_Success() {
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setName("Test Company");
        createCompanyDto.setRegistrationNumber("12345");
        createCompanyDto.setTaxId("67890");
        createCompanyDto.setActivityCode("1");
        createCompanyDto.setAddress("Test Address");
        createCompanyDto.setMajorityOwner(1L);

        Client client = new Client();
        client.setId(1L);

        ActivityCode activityCode = new ActivityCode();
        activityCode.setId("1");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(activityCodeRepository.findById("1")).thenReturn(Optional.of(activityCode));
        // Vraćamo "prazno" za registrationNumber i taxId, što znači da ne postoje u bazi
        when(companyRepository.findByRegistrationNumber("12345")).thenReturn(Optional.empty());
        when(companyRepository.findByTaxId("67890")).thenReturn(Optional.empty());
        // Kad sačuvamo, vraćamo isti objekat
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDto result = companyService.createCompany(createCompanyDto);

        assertNotNull(result);
        assertEquals("Test Company", result.getName());
        assertEquals("12345", result.getRegistrationNumber());
        assertEquals("67890", result.getTaxId());
        verify(clientRepository, times(1)).findById(1L);
        verify(activityCodeRepository, times(1)).findById("1");
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    void testCreateCompany_OwnerNotFound() {
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setMajorityOwner(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            companyService.createCompany(createCompanyDto);
        });

        assertEquals("Owner not found with ID: 1", exception.getMessage());
        verify(clientRepository, times(1)).findById(1L);
        verify(activityCodeRepository, never()).findById(any());
        verify(companyRepository, never()).save(any());
    }

    @Test
    void testCreateCompany_ActivityCodeNotFound() {
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setMajorityOwner(1L);
        createCompanyDto.setActivityCode("1");

        Client client = new Client();
        client.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(activityCodeRepository.findById("1")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            companyService.createCompany(createCompanyDto);
        });

        assertEquals("Activity code not found with ID: 1", exception.getMessage());
        verify(clientRepository, times(1)).findById(1L);
        verify(activityCodeRepository, times(1)).findById("1");
        verify(companyRepository, never()).save(any());
    }

    @Test
    void testCreateCompany_RegistrationNumberExists() {
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setMajorityOwner(1L);
        createCompanyDto.setActivityCode("10");
        createCompanyDto.setRegistrationNumber("12345");
        createCompanyDto.setTaxId("99999");

        Client client = new Client();
        client.setId(1L);
        ActivityCode ac = new ActivityCode();
        ac.setId("10");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(activityCodeRepository.findById("10")).thenReturn(Optional.of(ac));
        when(companyRepository.findByRegistrationNumber("12345"))
                .thenReturn(Optional.of(new Company()));  // već postoji

        assertThrows(CompanyRegNumExistsException.class, () ->
                companyService.createCompany(createCompanyDto)
        );

        verify(companyRepository, never()).save(any());
    }

    @Test
    void testCreateCompany_TaxIdExists() {
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setMajorityOwner(2L);
        createCompanyDto.setActivityCode("20");
        createCompanyDto.setRegistrationNumber("12345");
        createCompanyDto.setTaxId("99999");

        Client client = new Client();
        client.setId(2L);
        ActivityCode ac = new ActivityCode();
        ac.setId("20");

        when(clientRepository.findById(2L)).thenReturn(Optional.of(client));
        when(activityCodeRepository.findById("20")).thenReturn(Optional.of(ac));
        when(companyRepository.findByRegistrationNumber("12345")).thenReturn(Optional.empty());
        when(companyRepository.findByTaxId("99999"))
                .thenReturn(Optional.of(new Company()));  // već postoji

        assertThrows(TaxIdAlreadyExistsException.class, () ->
                companyService.createCompany(createCompanyDto)
        );

        verify(companyRepository, never()).save(any());
    }


    // -------------------------------------------------------------------
    // getCompanyById(...)
    // -------------------------------------------------------------------
    @Test
    void testGetCompanyById_Success() {
        Long companyId = 1L;
        Company company = new Company();
        company.setId(companyId);
        company.setName("Test Company");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        CompanyDto result = companyService.getCompanyById(companyId);

        assertNotNull(result);
        assertEquals(companyId, result.getId());
        assertEquals("Test Company", result.getName());
        verify(companyRepository, times(1)).findById(companyId);
    }

    @Test
    void testGetCompanyById_NotFound() {
        Long companyId = 1L;
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () -> companyService.getCompanyById(companyId));
        verify(companyRepository, times(1)).findById(companyId);
    }

    // -------------------------------------------------------------------
    // getCompaniesForClientId(...)
    // -------------------------------------------------------------------
    @Test
    void testGetCompaniesForClientId_Success() {
        Long clientId = 1L;
        Company company1 = new Company();
        company1.setId(10L);
        company1.setName("Test Co 1");

        Company company2 = new Company();
        company2.setId(20L);
        company2.setName("Test Co 2");

        List<Company> companies = new ArrayList<>();
        companies.add(company1);
        companies.add(company2);

        when(companyRepository.findByMajorityOwner_Id(clientId)).thenReturn(companies);

        List<CompanyDto> result = companyService.getCompaniesForClientId(clientId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Co 1", result.get(0).getName());
        assertEquals("Test Co 2", result.get(1).getName());

        verify(companyRepository, times(1)).findByMajorityOwner_Id(clientId);
    }

    @Test
    void testGetCompaniesForClientId_Empty() {
        Long clientId = 999L;
        when(companyRepository.findByMajorityOwner_Id(clientId)).thenReturn(new ArrayList<>());

        List<CompanyDto> result = companyService.getCompaniesForClientId(clientId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(companyRepository, times(1)).findByMajorityOwner_Id(clientId);
    }
}

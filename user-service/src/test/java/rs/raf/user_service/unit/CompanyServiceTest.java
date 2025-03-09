package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.user_service.domain.dto.CreateCompanyDto;
import rs.raf.user_service.domain.entity.ActivityCode;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.domain.entity.Company;
import rs.raf.user_service.repository.ActivityCodeRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.CompanyRepository;
import rs.raf.user_service.service.CompanyService;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void testCreateCompany_Success() {
        // Arrange
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setName("Test Company");
        createCompanyDto.setRegistrationNumber("12345");

        createCompanyDto.setTaxId(String.valueOf(Long.valueOf("67890")));

        createCompanyDto.setActivityCode(String.valueOf(1L));
        createCompanyDto.setAddress("Test Address");
        createCompanyDto.setMajorityOwner(1L);

        Client client = new Client();
        client.setId(1L);

        ActivityCode activityCode = new ActivityCode();
        activityCode.setId(String.valueOf(1L));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(activityCodeRepository.findById(String.valueOf(1L))).thenReturn(Optional.of(activityCode));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        companyService.createCompany(createCompanyDto);

        // Assert
        verify(clientRepository, times(1)).findById(1L);
        verify(activityCodeRepository, times(1)).findById(String.valueOf(1L));
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    void testCreateCompany_OwnerNotFound() {
        // Arrange
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setMajorityOwner(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
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
        // Arrange
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setMajorityOwner(1L);
        createCompanyDto.setActivityCode(String.valueOf(1L));

        Client client = new Client();
        client.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(activityCodeRepository.findById(String.valueOf(1L))).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            companyService.createCompany(createCompanyDto);
        });

        assertEquals("Activity code not found with ID: 1", exception.getMessage());
        verify(clientRepository, times(1)).findById(1L);
        verify(activityCodeRepository, times(1)).findById(String.valueOf(1L));
        verify(companyRepository, never()).save(any());
    }
}
package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.user_service.domain.dto.AuthorizedPersonelDto;
import rs.raf.user_service.domain.dto.CreateAuthorizedPersonelDto;
import rs.raf.user_service.domain.entity.AuthorizedPersonel;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.domain.entity.Company;
import rs.raf.user_service.domain.mapper.AuthorizedPersonelMapper;
import rs.raf.user_service.repository.AuthorizedPersonelRepository;
import rs.raf.user_service.repository.CompanyRepository;
import rs.raf.user_service.service.AuthorizedPersonelService;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthorizedPersonelServiceTest {

    @Mock
    private AuthorizedPersonelRepository authorizedPersonelRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuthorizedPersonelMapper authorizedPersonelMapper;

    @InjectMocks
    private AuthorizedPersonelService authorizedPersonelService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createAuthorizedPersonel_Success() {
        // Arrange
        CreateAuthorizedPersonelDto createDto = new CreateAuthorizedPersonelDto();
        createDto.setFirstName("John");
        createDto.setLastName("Doe");
//         createDto.setDateOfBirth(LocalDate.of(2000, 1, 1).toEpochDay());
        createDto.setDateOfBirth(LocalDate.of(2000,1,1)); // 2000-01-01
        createDto.setGender("Male");
        createDto.setEmail("john.doe@example.com");
        createDto.setPhoneNumber("1234567890");
        createDto.setAddress("123 Main St");
        createDto.setCompanyId(1L);

        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        AuthorizedPersonel authorizedPersonel = new AuthorizedPersonel();
        authorizedPersonel.setId(1L);
        authorizedPersonel.setFirstName("John");
        authorizedPersonel.setCompany(company);

        AuthorizedPersonelDto expectedDto = new AuthorizedPersonelDto();
        expectedDto.setId(1L);
        expectedDto.setFirstName("John");
        expectedDto.setCompanyId(1L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(authorizedPersonelMapper.toEntity(any(CreateAuthorizedPersonelDto.class), any(Company.class)))
                .thenReturn(authorizedPersonel);
        when(authorizedPersonelRepository.save(any(AuthorizedPersonel.class))).thenReturn(authorizedPersonel);
        when(authorizedPersonelMapper.toDto(any(AuthorizedPersonel.class))).thenReturn(expectedDto);

        // Act
        AuthorizedPersonelDto result = authorizedPersonelService.createAuthorizedPersonel(createDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals(1L, result.getCompanyId());
        verify(companyRepository).findById(1L);
        verify(authorizedPersonelMapper).toEntity(any(CreateAuthorizedPersonelDto.class), any(Company.class));
        verify(authorizedPersonelRepository).save(any(AuthorizedPersonel.class));
        verify(authorizedPersonelMapper).toDto(any(AuthorizedPersonel.class));
    }

    @Test
    void createAuthorizedPersonel_CompanyNotFound() {
        // Arrange
        CreateAuthorizedPersonelDto createDto = new CreateAuthorizedPersonelDto();
        createDto.setCompanyId(1L);

        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> authorizedPersonelService.createAuthorizedPersonel(createDto));
        verify(companyRepository).findById(1L);
        verify(authorizedPersonelMapper, never()).toEntity(any(), any());
        verify(authorizedPersonelRepository, never()).save(any());
    }

    @Test
    void getAuthorizedPersonelByCompany_Success() {
        // Arrange
        Company company = new Company();
        company.setId(1L);

        AuthorizedPersonel person1 = new AuthorizedPersonel();
        person1.setId(1L);
        person1.setFirstName("John");

        AuthorizedPersonel person2 = new AuthorizedPersonel();
        person2.setId(2L);
        person2.setFirstName("Jane");

        List<AuthorizedPersonel> personelList = List.of(person1, person2);

        AuthorizedPersonelDto dto1 = new AuthorizedPersonelDto();
        dto1.setId(1L);
        dto1.setFirstName("John");

        AuthorizedPersonelDto dto2 = new AuthorizedPersonelDto();
        dto2.setId(2L);
        dto2.setFirstName("Jane");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(authorizedPersonelRepository.findByCompany(company)).thenReturn(personelList);
        when(authorizedPersonelMapper.toDto(person1)).thenReturn(dto1);
        when(authorizedPersonelMapper.toDto(person2)).thenReturn(dto2);

        // Act
        List<AuthorizedPersonelDto> result = authorizedPersonelService.getAuthorizedPersonelByCompany(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals(2L, result.get(1).getId());
        assertEquals("Jane", result.get(1).getFirstName());
        verify(companyRepository).findById(1L);
        verify(authorizedPersonelRepository).findByCompany(company);
        verify(authorizedPersonelMapper, times(2)).toDto(any(AuthorizedPersonel.class));
    }

    @Test
    void getAuthorizedPersonelById_Success() {
        // Arrange
        AuthorizedPersonel person = new AuthorizedPersonel();
        person.setId(1L);
        person.setFirstName("John");

        AuthorizedPersonelDto dto = new AuthorizedPersonelDto();
        dto.setId(1L);
        dto.setFirstName("John");

        when(authorizedPersonelRepository.findById(1L)).thenReturn(Optional.of(person));
        when(authorizedPersonelMapper.toDto(person)).thenReturn(dto);

        // Act
        AuthorizedPersonelDto result = authorizedPersonelService.getAuthorizedPersonelById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getFirstName());
        verify(authorizedPersonelRepository).findById(1L);
        verify(authorizedPersonelMapper).toDto(person);
    }

    @Test
    void getAuthorizedPersonelById_NotFound() {
        // Arrange
        when(authorizedPersonelRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> authorizedPersonelService.getAuthorizedPersonelById(1L));
        verify(authorizedPersonelRepository).findById(1L);
        verify(authorizedPersonelMapper, never()).toDto(any());
    }

    @Test
    void updateAuthorizedPersonel_Success() {
        // Arrange
        CreateAuthorizedPersonelDto updateDto = new CreateAuthorizedPersonelDto();
        updateDto.setFirstName("Updated John");
        updateDto.setLastName("Doe");
        updateDto.setCompanyId(1L);

        AuthorizedPersonel existingPerson = new AuthorizedPersonel();
        existingPerson.setId(1L);
        existingPerson.setFirstName("John");

        Company company = new Company();
        company.setId(1L);

        AuthorizedPersonelDto expectedDto = new AuthorizedPersonelDto();
        expectedDto.setId(1L);
        expectedDto.setFirstName("Updated John");
        expectedDto.setCompanyId(1L);

        when(authorizedPersonelRepository.findById(1L)).thenReturn(Optional.of(existingPerson));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(authorizedPersonelRepository.save(any(AuthorizedPersonel.class))).thenReturn(existingPerson);
        when(authorizedPersonelMapper.toDto(existingPerson)).thenReturn(expectedDto);

        // Act
        AuthorizedPersonelDto result = authorizedPersonelService.updateAuthorizedPersonel(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated John", result.getFirstName());
        verify(authorizedPersonelRepository).findById(1L);
        verify(companyRepository).findById(1L);
        verify(authorizedPersonelRepository).save(existingPerson);
        verify(authorizedPersonelMapper).toDto(existingPerson);
    }

    @Test
    void deleteAuthorizedPersonel_Success() {
        // Arrange
        when(authorizedPersonelRepository.existsById(1L)).thenReturn(true);

        // Act
        authorizedPersonelService.deleteAuthorizedPersonel(1L);

        // Assert
        verify(authorizedPersonelRepository).existsById(1L);
        verify(authorizedPersonelRepository).deleteById(1L);
    }

    @Test
    void deleteAuthorizedPersonel_NotFound() {
        // Arrange
        when(authorizedPersonelRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> authorizedPersonelService.deleteAuthorizedPersonel(1L));
        verify(authorizedPersonelRepository).existsById(1L);
        verify(authorizedPersonelRepository, never()).deleteById(any());
    }
}


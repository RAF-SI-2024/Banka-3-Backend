package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.raf.user_service.domain.dto.AuthorizedPersonelDto;
import rs.raf.user_service.domain.dto.ClientDto;
import rs.raf.user_service.domain.dto.CreateAuthorizedPersonelDto;
import rs.raf.user_service.controller.AuthorizedPersonelController;
import rs.raf.user_service.repository.AuthorizedPersonelRepository;
import rs.raf.user_service.service.AuthorizedPersonelService;
import rs.raf.user_service.service.ClientService;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AuthorizedPersonelControllerTest {

    @Mock
    private AuthorizedPersonelService authorizedPersonelService;

    @Mock
    private AuthorizedPersonelRepository authorizedPersonelRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthorizedPersonelController authorizedPersonelController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup security context mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createAuthorizedPersonnel_Success() {
        // Arrange
        CreateAuthorizedPersonelDto createDto = new CreateAuthorizedPersonelDto();
        createDto.setFirstName("John");
        createDto.setLastName("Doe");
        createDto.setCompanyId(1L);

        AuthorizedPersonelDto createdDto = new AuthorizedPersonelDto();
        createdDto.setId(1L);
        createdDto.setFirstName("John");
        createdDto.setLastName("Doe");
        createdDto.setCompanyId(1L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setEmail("client@example.com");

        when(authorizedPersonelService.createAuthorizedPersonel(createDto)).thenReturn(createdDto);

        // Act
        ResponseEntity<?> response = authorizedPersonelController.createAuthorizedPersonnel(createDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(createdDto, response.getBody());
        verify(authorizedPersonelService).createAuthorizedPersonel(createDto);
    }

    @Test
    void createAuthorizedPersonnel_EntityNotFound() {
        // Arrange
        CreateAuthorizedPersonelDto createDto = new CreateAuthorizedPersonelDto();
        createDto.setCompanyId(1L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setEmail("client@example.com");

        when(authorizedPersonelService.createAuthorizedPersonel(createDto))
                .thenThrow(new EntityNotFoundException("Company not found"));

        // Act
        ResponseEntity<?> response = authorizedPersonelController.createAuthorizedPersonnel(createDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Company not found", response.getBody());
        verify(authorizedPersonelService).createAuthorizedPersonel(createDto);
    }

    @Test
    void getAuthorizedPersonnelByCompany_Success() {
        // Arrange
        AuthorizedPersonelDto dto1 = new AuthorizedPersonelDto();
        dto1.setId(1L);
        dto1.setFirstName("John");

        AuthorizedPersonelDto dto2 = new AuthorizedPersonelDto();
        dto2.setId(2L);
        dto2.setFirstName("Jane");

        List<AuthorizedPersonelDto> dtoList = Arrays.asList(dto1, dto2);

        when(authorizedPersonelService.getAuthorizedPersonelByCompany(1L)).thenReturn(dtoList);

        // Act
        ResponseEntity<?> response = authorizedPersonelController.getAuthorizedPersonnelByCompany(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dtoList, response.getBody());
        verify(authorizedPersonelService).getAuthorizedPersonelByCompany(1L);
    }

    @Test
    void getAuthorizedPersonnelByCompany_NotFound() {
        // Arrange
        when(authorizedPersonelService.getAuthorizedPersonelByCompany(1L))
                .thenThrow(new EntityNotFoundException("Company not found"));

        // Act
        ResponseEntity<?> response = authorizedPersonelController.getAuthorizedPersonnelByCompany(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Company not found", response.getBody());
        verify(authorizedPersonelService).getAuthorizedPersonelByCompany(1L);
    }

    @Test
    void getAuthorizedPersonnelById_Success() {
        // Arrange
        AuthorizedPersonelDto dto = new AuthorizedPersonelDto();
        dto.setId(1L);
        dto.setFirstName("John");

        when(authorizedPersonelService.getAuthorizedPersonelById(1L)).thenReturn(dto);

        // Act
        ResponseEntity<?> response = authorizedPersonelController.getAuthorizedPersonnelById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
        verify(authorizedPersonelService).getAuthorizedPersonelById(1L);
    }

    @Test
    void getAuthorizedPersonnelById_NotFound() {
        // Arrange
        when(authorizedPersonelService.getAuthorizedPersonelById(1L))
                .thenThrow(new EntityNotFoundException("Authorized personnel not found"));

        // Act
        ResponseEntity<?> response = authorizedPersonelController.getAuthorizedPersonnelById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Authorized personnel not found", response.getBody());
        verify(authorizedPersonelService).getAuthorizedPersonelById(1L);
    }

    @Test
    void updateAuthorizedPersonnel_Success() {
        // Arrange
        CreateAuthorizedPersonelDto updateDto = new CreateAuthorizedPersonelDto();
        updateDto.setFirstName("Updated John");
        updateDto.setCompanyId(1L);

        AuthorizedPersonelDto updatedDto = new AuthorizedPersonelDto();
        updatedDto.setId(1L);
        updatedDto.setFirstName("Updated John");
        updatedDto.setCompanyId(1L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setEmail("client@example.com");

        when(authorizedPersonelService.updateAuthorizedPersonel(1L, updateDto)).thenReturn(updatedDto);

        // Act
        ResponseEntity<?> response = authorizedPersonelController.updateAuthorizedPersonnel(1L, updateDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedDto, response.getBody());
        verify(authorizedPersonelService).updateAuthorizedPersonel(1L, updateDto);
    }

    @Test
    void deleteAuthorizedPersonnel_Success() {
        // Arrange
        AuthorizedPersonelDto dto = new AuthorizedPersonelDto();
        dto.setId(1L);
        dto.setCompanyId(1L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setEmail("client@example.com");

        doNothing().when(authorizedPersonelService).deleteAuthorizedPersonel(1L);

        // Act
        ResponseEntity<?> response = authorizedPersonelController.deleteAuthorizedPersonnel(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(authorizedPersonelService).deleteAuthorizedPersonel(1L);
    }

    // @todo nek neko fixa ovo hvala
//    @Test
//    void deleteAuthorizedPersonnel_NotFound() {
//        // Arrange
//        when(authorizedPersonelRepository.existsById(1L)).thenReturn(false);
//
//        // Act
//        ResponseEntity<?> response = authorizedPersonelController.deleteAuthorizedPersonnel(1L);
//
//        // Assert
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//        assertEquals("Authorized personnel not found", response.getBody());
//        verify(authorizedPersonelService, never()).deleteAuthorizedPersonel(anyLong());
//    }
}
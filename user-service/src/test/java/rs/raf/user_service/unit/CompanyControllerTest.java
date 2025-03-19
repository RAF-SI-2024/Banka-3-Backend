package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.CompanyController;
import rs.raf.user_service.domain.dto.CompanyDto;
import rs.raf.user_service.domain.dto.CreateCompanyDto;
import rs.raf.user_service.domain.dto.ErrorMessageDto;
import rs.raf.user_service.service.CompanyService;

import rs.raf.user_service.exceptions.ClientNotFoundException;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private CompanyController companyController;

    private List<CompanyDto> mockCompanies;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockCompanies = Arrays.asList(
                new CompanyDto(1L, "Company A", "11", "22", "33", "adresa", 1L),
                new CompanyDto(2L, "Company B","11", "22", "33", "adresa", 1L)
        );
    }

    @Test
    public void testCreateCompany_Success() {
        // Arrange
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setName("Test Company");

        // Act
        ResponseEntity<?> response = companyController.createCompany(createCompanyDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(companyService).createCompany(createCompanyDto);
    }

    @Test
    public void testCreateCompany_Failure() {
        // Arrange
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setName("Test Company");


        Long clientId = 1L;
        String expectedErrorMessage = "Cannot find client with id: " + clientId;
        doThrow(new ClientNotFoundException(clientId)).when(companyService).createCompany(any(CreateCompanyDto.class));


        // Act
        ResponseEntity<?> response = companyController.createCompany(createCompanyDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetCompaniesForClientId_Success() {
        Long clientId = 1L;
        when(companyService.getCompaniesForClientId(clientId)).thenReturn(mockCompanies);

        ResponseEntity<?> response = companyController.getCompaniesForClientId(clientId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockCompanies, response.getBody());
    }

    @Test
    void testGetCompaniesForClientId_Failure() {
        Long clientId = 1L;
        when(companyService.getCompaniesForClientId(clientId)).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = companyController.getCompaniesForClientId(clientId);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ErrorMessageDto);
        assertEquals("Database error", ((ErrorMessageDto) response.getBody()).getError());
    }
}
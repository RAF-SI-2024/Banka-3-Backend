package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.CompanyController;
import rs.raf.user_service.dto.CreateCompanyDto;
import rs.raf.user_service.service.CompanyService;

import rs.raf.user_service.exceptions.ClientNotFoundException;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private CompanyController companyController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateCompany_Success() {
        // Arrange
        CreateCompanyDto createCompanyDto = new CreateCompanyDto();
        createCompanyDto.setName("Test Company");

        // Act
        ResponseEntity<String> response = companyController.createCompany(createCompanyDto);

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
        ResponseEntity<String> response = companyController.createCompany(createCompanyDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertEquals(expectedErrorMessage, response.getBody());

    }
}
package rs.raf.user_service.unit;

import org.junit.jupiter.api.Test;
import rs.raf.user_service.domain.dto.CompanyDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompanyDtoTest {

    @Test
    void testGettersAndSetters() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId(1L);
        companyDto.setName("Example Company");
        companyDto.setRegistrationNumber("123456");
        companyDto.setTaxId("789012");
        companyDto.setActivityCode("6201");
        companyDto.setAddress("123 Main St");

        assertEquals(1L, companyDto.getId());
        assertEquals("Example Company", companyDto.getName());
        assertEquals("123456", companyDto.getRegistrationNumber());
        assertEquals("789012", companyDto.getTaxId());
        assertEquals("6201", companyDto.getActivityCode());
        assertEquals("123 Main St", companyDto.getAddress());
    }

    @Test
    void testAllArgsConstructor() {
        CompanyDto companyDto = new CompanyDto(2L, "Another Co", "654321", "210987", "4791", "456 Second St");

        assertEquals(2L, companyDto.getId());
        assertEquals("Another Co", companyDto.getName());
        assertEquals("654321", companyDto.getRegistrationNumber());
        assertEquals("210987", companyDto.getTaxId());
        assertEquals("4791", companyDto.getActivityCode());
        assertEquals("456 Second St", companyDto.getAddress());
    }
}

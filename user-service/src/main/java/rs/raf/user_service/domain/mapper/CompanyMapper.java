package rs.raf.user_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.user_service.domain.dto.CompanyDto;
import rs.raf.user_service.domain.entity.Company;

@Component
public class CompanyMapper {

    // ✅ Mapiranje iz Company u CompanyDto (sa svim poljima)
    public static CompanyDto toDto(Company company) {
        if (company == null) return null;
        return new CompanyDto(
                company.getId(),
                company.getName(),
                company.getRegistrationNumber(),
                company.getTaxId(),
                company.getActivityCode(),
                company.getAddress()
        );
    }
}

package rs.raf.user_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.user_service.domain.dto.AuthorizedPersonelDto;
import rs.raf.user_service.domain.dto.CreateAuthorizedPersonelDto;
import rs.raf.user_service.domain.entity.AuthorizedPersonel;
import rs.raf.user_service.domain.entity.Company;

@Component
public class AuthorizedPersonelMapper {

    public AuthorizedPersonelDto toDto(AuthorizedPersonel authorizedPersonel) {
        if (authorizedPersonel == null) {
            return null;
        }

        AuthorizedPersonelDto dto = new AuthorizedPersonelDto();
        dto.setId(authorizedPersonel.getId());
        dto.setFirstName(authorizedPersonel.getFirstName());
        dto.setLastName(authorizedPersonel.getLastName());
        dto.setDateOfBirth(authorizedPersonel.getDateOfBirth());
        dto.setGender(authorizedPersonel.getGender());
        dto.setEmail(authorizedPersonel.getEmail());
        dto.setPhoneNumber(authorizedPersonel.getPhoneNumber());
        dto.setAddress(authorizedPersonel.getAddress());

        if (authorizedPersonel.getCompany() != null) {
            dto.setCompanyId(authorizedPersonel.getCompany().getId());
        }

        return dto;
    }

    public AuthorizedPersonel toEntity(CreateAuthorizedPersonelDto dto, Company company) {
        if (dto == null) {
            return null;
        }

        AuthorizedPersonel authorizedPersonel = new AuthorizedPersonel();
        authorizedPersonel.setFirstName(dto.getFirstName());
        authorizedPersonel.setLastName(dto.getLastName());
        authorizedPersonel.setDateOfBirth(dto.getDateOfBirth());
        authorizedPersonel.setGender(dto.getGender());
        authorizedPersonel.setEmail(dto.getEmail());
        authorizedPersonel.setPhoneNumber(dto.getPhoneNumber());
        authorizedPersonel.setAddress(dto.getAddress());
        authorizedPersonel.setCompany(company);

        return authorizedPersonel;
    }
}
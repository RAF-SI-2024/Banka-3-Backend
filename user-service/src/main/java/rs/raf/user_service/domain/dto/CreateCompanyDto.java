package rs.raf.user_service.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class CreateCompanyDto {

    private String name;
    private String registrationNumber;
    private String taxId;
    private String activityCode;
    private String address;
    private Long majorityOwner;
}

package rs.raf.user_service.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Getter
@Setter
@NoArgsConstructor
public class CreateCompanyDto {
    @NotNull(message = "Name must not be null")
    @NotBlank
    private String name;
    @NotNull(message = "registrationNumber must not be null")
    @NotBlank
    private String registrationNumber;
    @NotNull(message = "taxId must not be null")
    @NotBlank
    private String taxId;
    @NotNull(message = "activityCode must not be null")
    @NotBlank
    private String activityCode;
    @NotNull(message = "address must not be null")
    @NotBlank
    private String address;
    @NotNull(message = "majorityOwner must not be null")
    @Min(1L)
    private Long majorityOwner;
}

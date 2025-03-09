package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CompanyDto {
    private Long id;
    private String name;
    private String registrationNumber;
    private String taxId;
    private String activityCode;
    private String address;
    private ClientDto majorityOwner;
}
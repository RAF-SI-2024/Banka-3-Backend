package rs.raf.bank_service.domain.dto;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteTransferDto {
    private Long clientId;
    private TransferDto createTransferDto;
}


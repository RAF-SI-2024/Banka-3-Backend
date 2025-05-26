package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bank2TransactionCodeListDto {
    private List<Bank2TransactionCodeDetailsDto> items;
}

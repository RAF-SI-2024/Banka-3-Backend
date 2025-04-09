package rs.raf.bank_service.domain.dto;

import lombok.*;

import java.util.List;

@Data
public class Banka2AccountResponseDto {
    private List<Banka2AccountItemDto> items;
    private int pageNumber;
    private int pageSize;
    private int totalElements;
    private int totalPages;
}

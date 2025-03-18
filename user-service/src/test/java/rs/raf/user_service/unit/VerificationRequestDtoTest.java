package rs.raf.user_service.unit;

import org.junit.jupiter.api.Test;
import rs.raf.user_service.domain.dto.VerificationRequestDto;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VerificationRequestDtoTest {

    @Test
    void testGettersAndSetters() {
        VerificationRequestDto dto = new VerificationRequestDto();

        LocalDateTime now = LocalDateTime.now();
        dto.setUserId(5L);
        dto.setExpirationTime(now.plusHours(1));
        dto.setTargetId(100L);
        dto.setStatus(VerificationStatus.PENDING);
        dto.setVerificationType(VerificationType.CHANGE_LIMIT);
        dto.setCreatedAt(now);

        assertEquals(5L, dto.getUserId());
        assertEquals(now.plusHours(1), dto.getExpirationTime());
        assertEquals(100L, dto.getTargetId());
        assertEquals(VerificationStatus.PENDING, dto.getStatus());
        assertEquals(VerificationType.CHANGE_LIMIT, dto.getVerificationType());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();
        VerificationRequestDto dto = VerificationRequestDto.builder()
                .userId(1L)
                .expirationTime(now)
                .targetId(200L)
                .status(VerificationStatus.APPROVED)
                .verificationType(VerificationType.LOAN)
                .createdAt(now.minusDays(1))
                .build();

        assertEquals(1L, dto.getUserId());
        assertEquals(now, dto.getExpirationTime());
        assertEquals(200L, dto.getTargetId());
        assertEquals(VerificationStatus.APPROVED, dto.getStatus());
        assertEquals(VerificationType.LOAN, dto.getVerificationType());
        assertEquals(now.minusDays(1), dto.getCreatedAt());
    }
}

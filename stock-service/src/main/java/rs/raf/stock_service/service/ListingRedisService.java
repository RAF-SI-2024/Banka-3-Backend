package rs.raf.stock_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ListingMapper listingMapper;
    private final ListingPriceHistoryRepository priceHistoryRepository;

    private static final String PREFIX = "listing:";

    public void save(Listing listing) {
        ListingDto dto = listingMapper.toDto(listing, priceHistoryRepository.findTopByListingOrderByDateDesc(listing));
        saveByTicker(dto);
    }

    public void saveByTicker(ListingDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(PREFIX + dto.getTicker(), json, Duration.ofHours(6));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ListingDto", e);
        }
    }

    public void saveAll(List<ListingDto> dtos) {
        for (ListingDto dto : dtos) {
            saveByTicker(dto);
        }
    }

    public ListingDto getByTicker(String ticker) {
        try {
            String json = redisTemplate.opsForValue().get(PREFIX + ticker);
            return json != null ? objectMapper.readValue(json, ListingDto.class) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize ListingDto", e);
        }
    }

    public List<ListingDto> getAllListings() {
        Set<String> keys = Optional.ofNullable(redisTemplate.keys(PREFIX + "*"))
                .map(set -> set.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        return keys.stream()
                .map(key -> {
                    try {
                        String json = redisTemplate.opsForValue().get(key);
                        return json != null ? objectMapper.readValue(json, ListingDto.class) : null;
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}

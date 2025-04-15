package rs.raf.stock_service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.ListingDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ListingRedisService {

    private static final String KEY_PREFIX = "listing:";

    private static final String TICKER_KEY_PREFIX = "listing:ticker:";

    public void saveByTicker(ListingDto dto) {
        redisTemplate.opsForValue().set(TICKER_KEY_PREFIX + dto.getTicker(), dto);
    }

    public ListingDto getByTicker(String ticker) {
        return redisTemplate.opsForValue().get(TICKER_KEY_PREFIX + ticker);
    }

    private final RedisTemplate<String, ListingDto> redisTemplate;

    public ListingRedisService(RedisTemplate<String, ListingDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveListing(ListingDto dto) {
        redisTemplate.opsForValue().set(KEY_PREFIX + dto.getId(), dto);
    }

    public void saveAll(List<ListingDto> listings) {
        listings.forEach(this::saveListing);
    }

    public Optional<ListingDto> getListingById(Long id) {
        ListingDto dto = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        return Optional.ofNullable(dto);
    }

    public List<ListingDto> getAllListings() {
        return redisTemplate.keys(KEY_PREFIX + "*").stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public void updateListing(ListingDto dto) {
        redisTemplate.opsForValue().set(KEY_PREFIX + dto.getId(), dto);
    }
}

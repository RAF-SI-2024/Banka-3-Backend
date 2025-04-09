package rs.raf.stock_service.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ActuaryLimitDto;
import rs.raf.stock_service.domain.dto.ClientDto;
import rs.raf.stock_service.exceptions.ActuaryLimitNotFoundException;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {

            @Override
            public ActuaryLimitDto getActuaryByEmployeeId(Long id) {
                if(cause instanceof FeignException.NotFound)
                    throw new ActuaryLimitNotFoundException(id);
                throw new RuntimeException(cause);
            }

            @Override
            public ClientDto getClientById(Long id) {
                return null;
            }
        };
    }
}

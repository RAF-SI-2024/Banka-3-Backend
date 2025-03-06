package rs.raf.user_service.domain.mapper;

import rs.raf.user_service.domain.dto.UserDto;
import rs.raf.user_service.domain.entity.BaseUser;

public class UserMapper {
    public static UserDto toDto(BaseUser baseUser) {
        if (baseUser == null) return null;
        return new UserDto(
                baseUser.getId(),
                baseUser.getFirstName(),
                baseUser.getLastName(),
                baseUser.getEmail(),
                baseUser.getAddress(),
                baseUser.getPhone(),
                baseUser.getGender(),
                baseUser.getBirthDate(),
                baseUser.getJmbg()
        );
    }
}

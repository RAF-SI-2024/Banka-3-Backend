package rs.raf.user_service.mapper;

import rs.raf.user_service.dto.ClientDto;
import rs.raf.user_service.dto.UserDto;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Client;

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

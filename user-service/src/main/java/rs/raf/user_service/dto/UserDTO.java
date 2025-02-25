package rs.raf.user_service.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class UserDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    // Employee specifična polja (null za Client)
    private String username;
    private Boolean active;
    private String position;
    private String department;

    public UserDTO(Long id, String firstName, String lastName, String email,
                   String username, Boolean active, String position, String department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.username = username;
        this.active = active;
        this.position = position;
        this.department = department;
    }
}

package rs.raf.user_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.user_service.controller.UserController;
import rs.raf.user_service.dto.UserDTO;
import rs.raf.user_service.service.UserService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper(); //
    private MockMvc mockMvc;
    @Mock
    private UserService userService;
    @InjectMocks
    private UserController userController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    public void testGetAllUsers_Success() throws Exception {
        when(userService.listUsers(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService, times(1)).listUsers(0, 5);
    }

    @Test
    public void testGetUserById_Success() throws Exception {
        Long userId = 1L;
        UserDTO userDto = new UserDTO();
        userDto.setId(userId);
        userDto.setEmail("test@example.com");

        when(userService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    public void testGetUserById_NotFound() throws Exception {
        Long userId = 2L;
        when(userService.getUserById(userId)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    public void testDeleteUser_Success() throws Exception {
        Long userId = 1L;

        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    public void testGetUserById_Success_WithMoreFields() throws Exception {
        Long userId = 1L;
        UserDTO userDto = new UserDTO();
        userDto.setId(userId);
        userDto.setEmail("test@example.com");
        userDto.setUsername("testuser");

        when(userService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser")); // ✅ dodatna provera

        verify(userService, times(1)).getUserById(userId);
    }

    //  Test za dodavanje korisnika
    @Test
    public void addUser_Success() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "Petar", "Petrović", "petar.p@example.com",
                "petar90", true, "Manager", "HR");

        when(userService.addUser(any(UserDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userDTO.getId()))
                .andExpect(jsonPath("$.firstName").value(userDTO.getFirstName()));

        verify(userService, times(1)).addUser(any(UserDTO.class));
    }

    //  Test za uspešno ažuriranje korisnika
    @Test
    public void updateUser_Success() throws Exception {
        Long userId = 1L;
        UserDTO updatedUser = new UserDTO(userId, "Marko", "Marković", "marko.m@example.com",
                "marko90", true, "Developer", "IT");

        when(userService.updateUser(eq(userId), any(UserDTO.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value(updatedUser.getFirstName()));

        verify(userService, times(1)).updateUser(eq(userId), any(UserDTO.class));
    }

    //  Test za ažuriranje korisnika koji ne postoji
    @Test
    public void updateUser_NotFound() throws Exception {
        Long userId = 100L;
        UserDTO userDTO = new UserDTO(userId, "Nikola", "Nikolić", "nikola.n@example.com",
                "nikola90", true, "Tester", "QA");

        when(userService.updateUser(eq(userId), any(UserDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq(userId), any(UserDTO.class));
    }
}




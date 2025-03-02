package rs.raf.bank_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import okhttp3.Response;
import rs.raf.bank_service.domain.dto.UserDto;

import java.io.IOException;

@Service
public class UserService {
    @Value("http://localhost:8080/api/") // za sad hardcoded ovde
    private String USER_SERVICE_URL;
    private final OkHttpClient client;

    public UserService() {
        this.client = new OkHttpClient();
    }

    public UserDto getUserById(Long id, String token) {
        Request request = new Request.Builder()
                .url(USER_SERVICE_URL + "admin/clients/" + id)
                .header("Authorization", token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return parseResponse(response.body().string());
            } else {
                return null;
            }
        } catch (IOException | RuntimeException ie) {
            return null;
        }

    }

    public UserDto parseResponse(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(responseBody, UserDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}

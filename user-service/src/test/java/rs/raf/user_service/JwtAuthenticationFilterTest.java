package rs.raf.user_service;

import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import rs.raf.user_service.security.JwtAuthenticationFilter;
import rs.raf.user_service.utils.JwtTokenUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterTest {
    private JwtTokenUtil jwtTokenUtil;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    public void setup() {
        jwtTokenUtil = mock(JwtTokenUtil.class);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenUtil);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testDoFilter_ValidToken_Employee() throws ServletException, IOException {
        String email = "employee@example.com";
        String token = "validTokenEmployee";

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject(email);
        claims.put("permissions", Collections.singletonList("EMPLOYEE"));
        when(jwtTokenUtil.getClaimsFromToken(token)).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(email, userDetails.getUsername());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilter_ValidToken_Client() throws ServletException, IOException {
        String email = "client@example.com";
        String token = "validTokenClient";

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject(email);
        claims.put("permissions", Collections.singletonList("CLIENT"));
        when(jwtTokenUtil.getClaimsFromToken(token)).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(email, userDetails.getUsername());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilter_InvalidToken() throws ServletException, IOException {
        String token = "invalidToken";

        when(jwtTokenUtil.validateToken(token)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilter_NoToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

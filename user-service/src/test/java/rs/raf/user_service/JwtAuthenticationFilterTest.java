package rs.raf.user_service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.raf.user_service.configuration.JwtTokenUtil;
import rs.raf.user_service.security.JwtAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

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
        when(jwtTokenUtil.getSubjectFromToken(token)).thenReturn(email);
        Claims claims = new DefaultClaims();
        claims.setSubject(email);
        when(jwtTokenUtil.getClaimsFromToken(token)).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        // Call public doFilter, which calls doFilterInternal
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Is auth set
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilter_ValidToken_Client() throws ServletException, IOException {
        String email = "client@example.com";
        String token = "validTokenClient";

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.getSubjectFromToken(token)).thenReturn(email);
        Claims claims = new DefaultClaims();
        claims.setSubject(email);
        when(jwtTokenUtil.getClaimsFromToken(token)).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Is auth set
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
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

        // Is auth set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void testDoFilter_NoToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No Authorization header
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Auth shouldn't be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

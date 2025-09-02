package com.twitter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.dto.AuthRequest;
import com.twitter.dto.UserDto;
import com.twitter.model.User;
import com.twitter.security.JwtTokenProvider;
import com.twitter.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    private AuthRequest signUpRequest;
    private AuthRequest signInRequest;
    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        signUpRequest = new AuthRequest();
        signUpRequest.setUsername("testuser");
        signUpRequest.setEmail("test@example.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setDisplayName("Test User");

        signInRequest = new AuthRequest();
        signInRequest.setUsername("testuser");
        signInRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");

        testUserDto = new UserDto(testUser);
    }

    @Test
    void signUp_Success() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        String jwt = "test-jwt-token";

        when(userService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(jwt);
        when(userService.getUserProfile(testUser.getId(), null)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(jwt))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(userService).createUser("testuser", "test@example.com", "password123", "Test User");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication);
    }

    @Test
    void signUp_UsernameAlreadyExists() throws Exception {
        // Given
        when(userService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Username is already taken!"));

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Username is already taken!"));

        verify(userService).createUser("testuser", "test@example.com", "password123", "Test User");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void signIn_Success() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        String jwt = "test-jwt-token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn(jwt);
        when(userService.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(testUser));
        when(userService.getUserProfile(testUser.getId(), null)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(jwt))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication);
        verify(userService).findByUsernameOrEmail("testuser");
    }

    @Test
    void signIn_InvalidCredentials() throws Exception {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        // When & Then
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Invalid username or password!"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    void refreshToken_Success() throws Exception {
        // Given
        String oldJwt = "old-jwt-token";
        String newJwt = "new-jwt-token";
        Long userId = 1L;

        when(tokenProvider.validateToken(oldJwt)).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT(oldJwt)).thenReturn(userId);
        when(tokenProvider.generateTokenFromUserId(userId)).thenReturn(newJwt);
        when(userService.getUserProfile(userId, null)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + oldJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newJwt))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"));

        verify(tokenProvider).validateToken(oldJwt);
        verify(tokenProvider).getUserIdFromJWT(oldJwt);
        verify(tokenProvider).generateTokenFromUserId(userId);
    }

    @Test
    void refreshToken_InvalidToken() throws Exception {
        // Given
        String invalidJwt = "invalid-jwt-token";

        when(tokenProvider.validateToken(invalidJwt)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + invalidJwt))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Invalid token!"));

        verify(tokenProvider).validateToken(invalidJwt);
        verify(tokenProvider, never()).getUserIdFromJWT(anyString());
    }
}
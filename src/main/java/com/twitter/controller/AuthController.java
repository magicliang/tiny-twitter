package com.twitter.controller;

import com.twitter.dto.AuthRequest;
import com.twitter.dto.AuthResponse;
import com.twitter.dto.UserDto;
import com.twitter.model.User;
import com.twitter.security.JwtTokenProvider;
import com.twitter.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    AuthenticationManager authenticationManager;
    
    @Autowired
    UserService userService;
    
    @Autowired
    JwtTokenProvider tokenProvider;
    
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String jwt = tokenProvider.generateToken(authentication);
            
            User user = userService.findByUsernameOrEmail(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserDto userDto = userService.getUserProfile(user.getId(), null);
            
            return ResponseEntity.ok(new AuthResponse(jwt, userDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error: Invalid username or password!");
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthRequest signUpRequest) {
        try {
            User user = userService.createUser(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                signUpRequest.getPassword(),
                signUpRequest.getDisplayName()
            );
            
            // Auto login after registration
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    signUpRequest.getUsername(),
                    signUpRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String jwt = tokenProvider.generateToken(authentication);
            
            UserDto userDto = userService.getUserProfile(user.getId(), null);
            
            return ResponseEntity.ok(new AuthResponse(jwt, userDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7); // Remove "Bearer " prefix
            if (tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromJWT(jwt);
                String newJwt = tokenProvider.generateTokenFromUserId(userId);
                
                UserDto userDto = userService.getUserProfile(userId, null);
                
                return ResponseEntity.ok(new AuthResponse(newJwt, userDto));
            } else {
                return ResponseEntity.badRequest()
                        .body("Error: Invalid token!");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }
}
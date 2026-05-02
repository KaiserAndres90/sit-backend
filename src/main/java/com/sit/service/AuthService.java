package com.sit.service;

import com.sit.config.JwtUtil;
import com.sit.dto.AuthRequest;
import com.sit.dto.AuthResponse;
import com.sit.dto.RegisterRequest;
import com.sit.dto.UserResponse;
import com.sit.exception.BadRequestException;
import com.sit.exception.ResourceNotFoundException;
import com.sit.model.User;
import com.sit.repository.UserRepository;
import com.sit.security.UserPrincipal;
import com.sit.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("Usuario inactivo");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);
        log.info("Login exitoso: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo ya está registrado");
        }
        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = UserService.generateRandomPassword();
            log.info("[SIMULACIÓN EMAIL] Contraseña temporal para {} enviada al correo: {}", request.getEmail(), rawPassword);
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .role(request.getRole())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        log.info("Usuario registrado vía /auth/register: {}", user.getEmail());

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        UserPrincipal p = SecurityUtils.currentPrincipal();
        User user = userRepository.findById(p.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return UserService.toUserResponse(user);
    }
}

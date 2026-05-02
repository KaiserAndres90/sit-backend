package com.sit.service;

import com.sit.dto.UserCreateRequest;
import com.sit.dto.UserCreateResultResponse;
import com.sit.dto.UserResponse;
import com.sit.dto.UserUpdateRequest;
import com.sit.exception.BadRequestException;
import com.sit.exception.ResourceNotFoundException;
import com.sit.model.Role;
import com.sit.model.User;
import com.sit.repository.TicketRepository;
import com.sit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public static String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserService::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return toUserResponse(user);
    }

    @Transactional
    public UserCreateResultResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo ya está registrado");
        }
        String temp = generateRandomPassword();
        log.info("[SIMULACIÓN EMAIL] Nueva cuenta {} — contraseña temporal enviada por correo: {}", request.getEmail(), temp);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(temp))
                .role(request.getRole())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        return UserCreateResultResponse.builder()
                .user(toUserResponse(user))
                .temporaryPassword(temp)
                .build();
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo ya está en uso");
        }
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (user.getRole() == Role.ADMINISTRADOR && userRepository.countByRole(Role.ADMINISTRADOR) <= 1) {
            throw new BadRequestException("No se puede eliminar al último usuario administrador");
        }

        long tickets = ticketRepository.countByCreatedByIdOrAssignedToId(id, id);
        if (tickets > 0) {
            throw new BadRequestException("No se puede eliminar el usuario: tiene tickets asociados");
        }

        userRepository.delete(user);
        log.info("Usuario eliminado id={}", id);
    }

    @Transactional
    public TemporaryPasswordResult resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        String temp = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(temp));
        userRepository.save(user);
        log.info("[SIMULACIÓN EMAIL] Restablecimiento de contraseña para {} — nueva temporal: {}", user.getEmail(), temp);
        return new TemporaryPasswordResult(temp);
    }

    public record TemporaryPasswordResult(String temporaryPassword) {
    }
}

package com.sit.config;

import com.sit.model.Role;
import com.sit.model.User;
import com.sit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Crea un administrador por defecto si la base de datos está vacía (desarrollo).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = User.builder()
                .name("Administrador SIT")
                .email("admin@sit.com")
                .password(passwordEncoder.encode("Admin123!"))
                .role(Role.ADMINISTRADOR)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
        log.info("Base vacía: creado usuario admin@sit.com con contraseña temporal Admin123! (cambie en producción)");
    }
}

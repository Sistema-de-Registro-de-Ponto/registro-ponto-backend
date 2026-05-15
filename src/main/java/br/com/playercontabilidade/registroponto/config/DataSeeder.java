package br.com.playercontabilidade.registroponto.config;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.User;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ColaboratorRepository colaboratorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUserAndColaborator("colaborador", "12345678", Role.COLLABORATOR, "Natanael");
        seedUserAndColaborator("gerente", "87654321", Role.MANAGER, "Gerente");
    }

    private void seedUserAndColaborator(String username, String rawPassword, Role role, String firstName) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User created = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(rawPassword))
                            .role(role)
                            .build();
                    User saved = userRepository.save(created);
                    log.info("Seed: criou usuário '{}' com role {}", username, role);
                    return saved;
                });

        if (!colaboratorRepository.existsByUser_Id(user.getId())) {
            colaboratorRepository.save(Colaborator.builder()
                    .user(user)
                    .firstName(firstName)
                    .build());
            log.info("Seed: criou colaborador para '{}'", username);
        }
    }
}

package br.com.playercontabilidade.registroponto.config;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Manager;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.User;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.ManagerRepository;
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
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedColaborator("colaborador", "12345678", "Natanael");
        seedManager("gerente", "87654321", "Gerente");
    }

    private void seedColaborator(String username, String rawPassword, String firstName) {
        User user = seedUser(username, rawPassword, Role.COLLABORATOR);
        if (!colaboratorRepository.existsByUser_Id(user.getId())) {
            colaboratorRepository.save(Colaborator.builder()
                    .user(user)
                    .firstName(firstName)
                    .build());
            log.info("Seed: criou colaborador para '{}'", username);
        }
    }

    private void seedManager(String username, String rawPassword, String firstName) {
        User user = seedUser(username, rawPassword, Role.MANAGER);
        if (!managerRepository.existsByUser_Id(user.getId())) {
            managerRepository.save(Manager.builder()
                    .user(user)
                    .firstName(firstName)
                    .build());
            log.info("Seed: criou gerente para '{}'", username);
        }
    }

    private User seedUser(String username, String rawPassword, Role role) {
        return userRepository.findByUsername(username)
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
    }
}

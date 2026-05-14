package br.com.playercontabilidade.registroponto.repository;

import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void deveBuscarPorUsername() {
        User user = User.builder()
                .username("bob")
                .password("$2a$10$dummyBcryptHashValueForTesting1234567890")
                .role(Role.MANAGER)
                .build();
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByUsername("bob");

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    void deveRetornarVazioQuandoUsernameNaoExiste() {
        Optional<User> found = userRepository.findByUsername("inexistente");

        assertThat(found).isEmpty();
    }

    @Test
    void deveInformarSeUsernameExiste() {
        User user = User.builder()
                .username("dave")
                .password("$2a$10$dummyBcryptHashValueForTesting1234567890")
                .role(Role.MANAGER)
                .build();
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByUsername("dave")).isTrue();
        assertThat(userRepository.existsByUsername("eve")).isFalse();
    }
}

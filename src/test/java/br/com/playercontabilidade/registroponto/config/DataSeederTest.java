package br.com.playercontabilidade.registroponto.config;

import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.User;
import br.com.playercontabilidade.registroponto.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void deveCriarAmbosUsuariosQuandoNenhumExiste() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded-" + inv.getArgument(0));

        dataSeeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());

        List<User> saved = captor.getAllValues();
        assertThat(saved)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("colaborador", "gerente");
        assertThat(saved)
                .extracting(User::getRole)
                .containsExactlyInAnyOrder(Role.COLLABORATOR, Role.MANAGER);
        assertThat(saved)
                .allSatisfy(u -> assertThat(u.getPassword()).startsWith("encoded-"));
    }

    @Test
    void naoDeveCriarUsuariosQuandoAmbosJaExistem() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void deveCriarApenasOUsuarioFaltante() throws Exception {
        when(userRepository.existsByUsername("colaborador")).thenReturn(true);
        when(userRepository.existsByUsername("gerente")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pwd");

        dataSeeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("gerente");
        assertThat(savedUser.getRole()).isEqualTo(Role.MANAGER);
    }
}

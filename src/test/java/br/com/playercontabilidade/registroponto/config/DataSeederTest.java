package br.com.playercontabilidade.registroponto.config;

import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Manager;
import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.User;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.ManagerRepository;
import br.com.playercontabilidade.registroponto.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private ColaboratorRepository colaboratorRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void deveCriarUsuariosColaboradorEGerenteQuandoNenhumExiste() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded-" + inv.getArgument(0));
        AtomicLong id = new AtomicLong(1);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id.getAndIncrement());
            return u;
        });
        when(colaboratorRepository.existsByUser_Id(anyLong())).thenReturn(false);
        when(managerRepository.existsByUser_Id(anyLong())).thenReturn(false);

        dataSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();
        assertThat(savedUsers)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("colaborador", "gerente");
        assertThat(savedUsers)
                .extracting(User::getRole)
                .containsExactlyInAnyOrder(Role.COLLABORATOR, Role.MANAGER);
        assertThat(savedUsers)
                .allSatisfy(u -> assertThat(u.getPassword()).startsWith("encoded-"));

        ArgumentCaptor<Colaborator> colabCaptor = ArgumentCaptor.forClass(Colaborator.class);
        verify(colaboratorRepository, times(1)).save(colabCaptor.capture());
        assertThat(colabCaptor.getValue().getUser().getUsername()).isEqualTo("colaborador");
        assertThat(colabCaptor.getValue().getFirstName()).isEqualTo("Natanael");

        ArgumentCaptor<Manager> managerCaptor = ArgumentCaptor.forClass(Manager.class);
        verify(managerRepository, times(1)).save(managerCaptor.capture());
        assertThat(managerCaptor.getValue().getUser().getUsername()).isEqualTo("gerente");
        assertThat(managerCaptor.getValue().getFirstName()).isEqualTo("Gerente");
    }

    @Test
    void naoDevePersistirNadaQuandoUsuariosPerfisJaExistem() {
        User colab = User.builder().id(1L).username("colaborador").password("x").role(Role.COLLABORATOR).build();
        User gerente = User.builder().id(2L).username("gerente").password("x").role(Role.MANAGER).build();
        when(userRepository.findByUsername("colaborador")).thenReturn(Optional.of(colab));
        when(userRepository.findByUsername("gerente")).thenReturn(Optional.of(gerente));
        when(colaboratorRepository.existsByUser_Id(1L)).thenReturn(true);
        when(managerRepository.existsByUser_Id(2L)).thenReturn(true);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(colaboratorRepository, never()).save(any());
        verify(managerRepository, never()).save(any());
    }

    @Test
    void deveCriarApenasGerenteQuandoUsuarioExisteSemPerfil() {
        User gerente = User.builder().id(2L).username("gerente").password("x").role(Role.MANAGER).build();
        User colab = User.builder().id(1L).username("colaborador").password("x").role(Role.COLLABORATOR).build();
        when(userRepository.findByUsername("colaborador")).thenReturn(Optional.of(colab));
        when(userRepository.findByUsername("gerente")).thenReturn(Optional.of(gerente));
        when(colaboratorRepository.existsByUser_Id(1L)).thenReturn(true);
        when(managerRepository.existsByUser_Id(2L)).thenReturn(false);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(colaboratorRepository, never()).save(any());
        ArgumentCaptor<Manager> captor = ArgumentCaptor.forClass(Manager.class);
        verify(managerRepository).save(captor.capture());
        assertThat(captor.getValue().getUser().getUsername()).isEqualTo("gerente");
        assertThat(captor.getValue().getFirstName()).isEqualTo("Gerente");
    }
}

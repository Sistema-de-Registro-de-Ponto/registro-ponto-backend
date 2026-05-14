package br.com.playercontabilidade.registroponto.service;

import br.com.playercontabilidade.registroponto.entity.Role;
import br.com.playercontabilidade.registroponto.entity.User;
import br.com.playercontabilidade.registroponto.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void deveCarregarColaboradorComAuthorityRoleCollaborator() {
        User user = User.builder()
                .id(1L)
                .username("colaborador")
                .password("$2a$10$hashedpassword")
                .role(Role.COLLABORATOR)
                .build();
        when(userRepository.findByUsername("colaborador")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("colaborador");

        assertThat(details.getUsername()).isEqualTo("colaborador");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashedpassword");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_COLLABORATOR");
    }

    @Test
    void deveCarregarGerenteComAuthorityRoleManager() {
        User user = User.builder()
                .id(2L)
                .username("gerente")
                .password("$2a$10$hashedpassword")
                .role(Role.MANAGER)
                .build();
        when(userRepository.findByUsername("gerente")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("gerente");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MANAGER");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inexistente"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inexistente");
    }
}

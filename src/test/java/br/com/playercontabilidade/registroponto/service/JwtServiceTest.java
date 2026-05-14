package br.com.playercontabilidade.registroponto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-que-tem-pelo-menos-256-bits-para-HS256-ok-aqui-vai-bastante-bytes";
    private static final long EXPIRATION_MS = 60_000L;

    private JwtService jwtService;
    private UserDetails colaborador;
    private UserDetails gerente;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
        colaborador = User.builder()
                .username("colaborador")
                .password("hash")
                .authorities("ROLE_COLLABORATOR")
                .build();
        gerente = User.builder()
                .username("gerente")
                .password("hash")
                .authorities("ROLE_MANAGER")
                .build();
    }

    @Test
    void deveGerarTokenComTresPartesSeparadasPorPonto() {
        String token = jwtService.generateToken(colaborador);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void deveExtrairUsernameDoTokenGerado() {
        String token = jwtService.generateToken(colaborador);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("colaborador");
    }

    @Test
    void deveValidarTokenParaUsuarioCorreto() {
        String token = jwtService.generateToken(colaborador);

        assertThat(jwtService.isTokenValid(token, colaborador)).isTrue();
    }

    @Test
    void naoDeveValidarTokenParaUsuarioDiferente() {
        String token = jwtService.generateToken(colaborador);

        assertThat(jwtService.isTokenValid(token, gerente)).isFalse();
    }

    @Test
    void naoDeveValidarTokenExpirado() {
        JwtService jaExpirado = new JwtService(SECRET, -1000L);
        String token = jaExpirado.generateToken(colaborador);

        assertThat(jwtService.isTokenValid(token, colaborador)).isFalse();
    }

    @Test
    void naoDeveValidarTokenComAssinaturaInvalida() {
        JwtService outroService = new JwtService(
                "outro-secret-totalmente-diferente-com-256-bits-no-minimo-blablabla-12345",
                EXPIRATION_MS);
        String tokenDeOutroService = outroService.generateToken(colaborador);

        assertThat(jwtService.isTokenValid(tokenDeOutroService, colaborador)).isFalse();
    }
}

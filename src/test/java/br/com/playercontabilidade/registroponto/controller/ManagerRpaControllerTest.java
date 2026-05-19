package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.LoginRequest;
import br.com.playercontabilidade.registroponto.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-manager-rpa;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.rpa.api-key=test-rpa-key"
})
class ManagerRpaControllerTest {

    private static final String IMPORT_PAYLOAD = """
            {
              "source_system": "ponto_agil",
              "records": [
                {
                  "external_employee_id": "001",
                  "employee_name": "Natanael",
                  "work_date": "2026-05-18",
                  "check_in_at": "2026-05-18T08:00:00-03:00",
                  "check_out_at": "2026-05-18T17:00:00-03:00"
                }
              ]
            }
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DirtiesContext
    void deveListarRegistrosRpaParaGerente() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "test-rpa-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IMPORT_PAYLOAD))
                .andExpect(status().isCreated());

        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2026-05-18")
                        .param("end_date", "2026-05-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employee_name").value("Natanael"))
                .andExpect(jsonPath("$.content[0].source_system").value("ponto_agil"))
                .andExpect(jsonPath("$.content[0].worked_seconds").value(32400))
                .andExpect(jsonPath("$.content[0].collaborator_id").isNumber())
                .andExpect(jsonPath("$.content[0].collaborator_first_name").value("Natanael"));
    }

    @Test
    void deveFiltrarRegistrosRpaPorNome() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "test-rpa-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IMPORT_PAYLOAD))
                .andExpect(status().isCreated());

        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2026-05-18")
                        .param("end_date", "2026-05-18")
                        .param("search", "nata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2026-05-18")
                        .param("end_date", "2026-05-18")
                        .param("search", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void deveRetornar403QuandoColaboradorListaRegistrosRpa() throws Exception {
        String colaboradorToken = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + colaboradorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar401AoListarRegistrosRpaSemToken() throws Exception {
        mockMvc.perform(get("/v1/manager/rpa/records"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar400QuandoStartDatePosteriorAEndDate() throws Exception {
        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2026-05-20")
                        .param("end_date", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaRegistrosNoPeriodo() throws Exception {
        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2000-01-01")
                        .param("end_date", "2000-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    @DirtiesContext
    void deveVincularColaboradorPorNomeIgnorandoMaiusculas() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "test-rpa-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source_system": "ponto_agil",
                                  "records": [
                                    {
                                      "employee_name": "natanael",
                                      "work_date": "2026-05-10",
                                      "check_in_at": "2026-05-10T08:00:00-03:00",
                                      "check_out_at": "2026-05-10T12:00:00-03:00"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());

        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/rpa/records")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2026-05-10")
                        .param("end_date", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].collaborator_first_name").value("Natanael"))
                .andExpect(jsonPath("$.content[0].worked_seconds").value(greaterThanOrEqualTo(0)));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest body = new LoginRequest(username, password);
        String responseBody = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        LoginResponse response = objectMapper.readValue(responseBody, LoginResponse.class);
        return response.token();
    }
}

package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.LoginRequest;
import br.com.playercontabilidade.registroponto.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-planned;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PlannedActivityControllerTest {

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
    void deveCriarListarERemoverAtividadePlanejada() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Revisar relatórios\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value("Revisar relatórios"))
                .andExpect(jsonPath("$.created_at").exists());

        mockMvc.perform(get("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].description").value("Revisar relatórios"))
                .andExpect(jsonPath("$[0].created_at").exists());

        String listBody = mockMvc.perform(get("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(listBody).get(0).get("id").asLong();

        mockMvc.perform(delete("/v1/activities/planned/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Revisar relatórios"))
                .andExpect(jsonPath("$.created_at").exists());

        mockMvc.perform(get("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deveRetornar404AoRemoverAtividadeInexistente() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(delete("/v1/activities/planned/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/activities/planned"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar400QuandoDescriptionVazia() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarUmaListaVaziaQuandoNaoHouverAtividadesPlanejadas() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(get("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deveRetornarUmaListaVaziaMesmoQueNaoTenhaSidoCheckadoEmUmaJornadaFinalizada() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Pendência não marcada\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys/current/end")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.journey_planned_activities[0].is_checked").value(false));

        mockMvc.perform(get("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
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

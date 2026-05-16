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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-journey-toggle;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JourneyPlannedActivityControllerTest {

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
    void deveMarcarEDesmarcarAtividadeNaJornada() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Revisar relatórios\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        String journeyBody = mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long journeyPlannedActivityId = objectMapper.readTree(journeyBody)
                .get("journey_planned_activities").get(0).get("id").asLong();
        long plannedActivityId = objectMapper.readTree(journeyBody)
                .get("journey_planned_activities").get(0).get("planned_activity_id").asLong();

        mockMvc.perform(put("/v1/journeys/activities/planned/" + journeyPlannedActivityId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_checked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(journeyPlannedActivityId))
                .andExpect(jsonPath("$.planned_activity_id").value(plannedActivityId))
                .andExpect(jsonPath("$.description").value("Revisar relatórios"))
                .andExpect(jsonPath("$.is_checked").value(true));

        mockMvc.perform(put("/v1/journeys/activities/planned/" + journeyPlannedActivityId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_checked\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_checked").value(false));
    }

    @Test
    void deveRetornar404QuandoItemNaoPertenceAJornadaAtiva() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(put("/v1/journeys/activities/planned/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_checked\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Atividade da jornada não encontrada"));
    }

    @Test
    void deveRetornar400QuandoCorpoInvalido() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(put("/v1/journeys/activities/planned/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(put("/v1/journeys/activities/planned/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_checked\":true}"))
                .andExpect(status().isUnauthorized());
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

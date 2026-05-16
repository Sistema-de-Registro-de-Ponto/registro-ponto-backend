package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.LoginRequest;
import br.com.playercontabilidade.registroponto.dto.LoginResponse;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
import br.com.playercontabilidade.registroponto.repository.UnplannedActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.Instant;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-journey-unplanned;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JourneyUnplannedActivityControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UnplannedActivityRepository unplannedActivityRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCriarListarNoCurrentERemoverAtividadeNaoPlanejada() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        String journeyJson = mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unplanned_activities.length()").value(0))
                .andReturn().getResponse().getContentAsString();

        long journeyId = objectMapper.readTree(journeyJson).get("id").asLong();

        mockMvc.perform(post("/v1/journeys/" + journeyId + "/activities/unplanned/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Suporte urgente\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.journey_id").value(journeyId))
                .andExpect(jsonPath("$.description").value("Suporte urgente"))
                .andExpect(jsonPath("$.created_at").exists());

        mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unplanned_activities.length()").value(1))
                .andExpect(jsonPath("$.unplanned_activities[0].description").value("Suporte urgente"))
                .andExpect(jsonPath("$.unplanned_activities[0].created_at").exists());

        long unplannedId = objectMapper.readTree(
                        mockMvc.perform(get("/v1/journeys/current")
                                        .header("Authorization", "Bearer " + token))
                                .andReturn().getResponse().getContentAsString())
                .get("unplanned_activities").get(0).get("id").asLong();

        mockMvc.perform(delete("/v1/journeys/activities/unplanned/" + unplannedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(unplannedId))
                .andExpect(jsonPath("$.journey_id").value(journeyId))
                .andExpect(jsonPath("$.description").value("Suporte urgente"));

        mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unplanned_activities.length()").value(0));
    }

    @Test
    void deveRetornar404QuandoJornadaNaoExisteOuNaoPertenceAoColaborador() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/journeys/99999/activities/unplanned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Jornada não encontrada"));
    }

    @Test
    void deveRetornar409QuandoJornadaNaoEstaEmAndamento() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        JsonNode root = objectMapper.readTree(mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
        long journeyId = root.get("id").asLong();

        Journey journey = journeyRepository.findById(journeyId).orElseThrow();
        journey.setStatus(JourneyStatus.COMPLETED);
        journey.setEndedAt(Instant.now());
        journeyRepository.save(journey);

        mockMvc.perform(post("/v1/journeys/" + journeyId + "/activities/unplanned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Não deve gravar\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Jornada não pode ser alterada"));
    }

    @Test
    void deveRetornar404AoRemoverAtividadeInexistente() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(delete("/v1/journeys/activities/unplanned/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Atividade não planejada não encontrada"));
    }

    @Test
    void deveRetornar409AoRemoverQuandoJornadaFinalizada() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        JsonNode root = objectMapper.readTree(mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
        long journeyId = root.get("id").asLong();

        mockMvc.perform(post("/v1/journeys/" + journeyId + "/activities/unplanned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Para remover\"}"))
                .andExpect(status().isCreated());

        long unplannedId = objectMapper.readTree(mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .get("unplanned_activities").get(0).get("id").asLong();

        Journey journey = journeyRepository.findById(journeyId).orElseThrow();
        journey.setStatus(JourneyStatus.COMPLETED);
        journey.setEndedAt(Instant.now());
        journeyRepository.save(journey);

        mockMvc.perform(delete("/v1/journeys/activities/unplanned/" + unplannedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Jornada não pode ser alterada"));

        unplannedActivityRepository.flush();
        org.junit.jupiter.api.Assertions.assertTrue(unplannedActivityRepository.findById(unplannedId).isPresent());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/journeys/1/activities/unplanned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"X\"}"))
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

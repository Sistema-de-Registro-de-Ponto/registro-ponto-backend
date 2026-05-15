package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.LoginRequest;
import br.com.playercontabilidade.registroponto.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-journey;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JourneyControllerTest {

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
    void deveIniciarJornadaComAtividadesPlanejadas() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Revisar relatórios\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Atender clientes\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.collaborator_id").isNumber())
                .andExpect(jsonPath("$.started_at").exists())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists())
                .andExpect(jsonPath("$.journey_planned_activities.length()").value(2))
                .andExpect(jsonPath("$.journey_planned_activities[0].id").isNumber())
                .andExpect(jsonPath("$.journey_planned_activities[0].planned_activity_id").isNumber())
                .andExpect(jsonPath("$.journey_planned_activities[0].description").value("Revisar relatórios"))
                .andExpect(jsonPath("$.journey_planned_activities[0].is_checked").value(false))
                .andExpect(jsonPath("$.journey_planned_activities[1].description").value("Atender clientes"))
                .andExpect(jsonPath("$.journey_planned_activities[1].is_checked").value(false));
    }

    @Test
    void deveIniciarJornadaSemAtividadesPlanejadas() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/journeys")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.journey_planned_activities").isEmpty());
    }

    @Test
    void deveRetornar409QuandoJaExisteJornadaEmAndamento() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/journeys")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Jornada em andamento"));
    }

    @Test
    void deveRetornarJornadaEmAndamento() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Revisar relatórios\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.started_at").exists())
                .andExpect(jsonPath("$.journey_planned_activities.length()").value(1))
                .andExpect(jsonPath("$.journey_planned_activities[0].description").value("Revisar relatórios"))
                .andExpect(jsonPath("$.journey_planned_activities[0].is_checked").value(false));
    }

    @Test
    void deveRetornar404QuandoNaoHaJornadaEmAndamento() throws Exception {
        String token = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Jornada não encontrada"));
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/journeys"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/journeys/current"))
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

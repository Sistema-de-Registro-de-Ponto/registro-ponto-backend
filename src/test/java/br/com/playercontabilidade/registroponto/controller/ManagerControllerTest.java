package br.com.playercontabilidade.registroponto.controller;

import br.com.playercontabilidade.registroponto.dto.LoginRequest;
import br.com.playercontabilidade.registroponto.dto.LoginResponse;
import br.com.playercontabilidade.registroponto.entity.Colaborator;
import br.com.playercontabilidade.registroponto.entity.Journey;
import br.com.playercontabilidade.registroponto.entity.JourneyStatus;
import br.com.playercontabilidade.registroponto.repository.ColaboratorRepository;
import br.com.playercontabilidade.registroponto.repository.JourneyRepository;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-manager;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ManagerControllerTest {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DIA_COM_DUAS_JORNADAS = LocalDate.of(2026, 5, 10);
    private static final LocalDate DIA_COM_UMA_JORNADA = LocalDate.of(2026, 5, 11);
    private static final long DURACAO_JORNADA_1_DIA_10 = 3600L;
    private static final long DURACAO_JORNADA_2_DIA_10 = 1800L;
    private static final long DURACAO_JORNADA_DIA_11 = 7200L;
    private static final long TOTAL_HORAS_DIA_10 =
            DURACAO_JORNADA_1_DIA_10 + DURACAO_JORNADA_2_DIA_10;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ColaboratorRepository colaboratorRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveRetornarPerfilDoGerenteComTokenValido() throws Exception {
        String token = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").isNumber())
                .andExpect(jsonPath("$.first_name").value("Gerente"));
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/v1/manager"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarOverviewVazioQuandoNaoHaJornadasNoPeriodo() throws Exception {
        String token = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + token)
                        .param("start_date", "2000-01-01")
                        .param("end_date", "2000-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(0))
                .andExpect(jsonPath("$.journeys_progress").value(0))
                .andExpect(jsonPath("$.average_adherence_percentage").value(0))
                .andExpect(jsonPath("$.activities_completed").value(0))
                .andExpect(jsonPath("$.unplanned_activities").value(0));
    }

    @Test
    @DirtiesContext
    void deveRetornarOverviewComMetricasAgregadas() throws Exception {
        String colaboradorToken = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarefa A\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarefa B\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + colaboradorToken))
                .andExpect(status().isCreated());

        String journeyBody = mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + colaboradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long journeyId = objectMapper.readTree(journeyBody).get("id").asLong();
        long firstActivityId = objectMapper.readTree(journeyBody)
                .get("journey_planned_activities").get(0).get("id").asLong();

        mockMvc.perform(put("/v1/journeys/activities/planned/" + firstActivityId)
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_checked\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/journeys/" + journeyId + "/activities/unplanned")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Imprevisto\"}"))
                .andExpect(status().isCreated());

        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2020-01-01")
                        .param("end_date", "2099-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(0))
                .andExpect(jsonPath("$.journeys_progress").value(1))
                .andExpect(jsonPath("$.average_adherence_percentage").value(50))
                .andExpect(jsonPath("$.activities_completed").value(1))
                .andExpect(jsonPath("$.unplanned_activities").value(1));

        mockMvc.perform(post("/v1/journeys/current/end")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2020-01-01")
                        .param("end_date", "2099-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.journeys_progress").value(0))
                .andExpect(jsonPath("$.average_adherence_percentage").value(50))
                .andExpect(jsonPath("$.activities_completed").value(1))
                .andExpect(jsonPath("$.unplanned_activities").value(1));
    }

    @Test
    void deveRetornar400QuandoStartDatePosteriorAEndDateNoOverview() throws Exception {
        String token = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + token)
                        .param("start_date", "2025-05-14")
                        .param("end_date", "2025-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornar401NoOverviewSemToken() throws Exception {
        mockMvc.perform(get("/v1/manager/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DirtiesContext
    void deveRetornarOverviewComOPeriodoInformado() throws Exception {
        String colaboradorToken = loginAndGetToken("colaborador", "12345678");

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarefa A\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/activities/planned")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarefa B\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/journeys/start")
                        .header("Authorization", "Bearer " + colaboradorToken))
                .andExpect(status().isCreated());

        String journeyBody = mockMvc.perform(get("/v1/journeys/current")
                        .header("Authorization", "Bearer " + colaboradorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long journeyId = objectMapper.readTree(journeyBody).get("id").asLong();
        long firstActivityId = objectMapper.readTree(journeyBody)
                .get("journey_planned_activities").get(0).get("id").asLong();

        mockMvc.perform(put("/v1/journeys/activities/planned/" + firstActivityId)
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_checked\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/journeys/" + journeyId + "/activities/unplanned")
                        .header("Authorization", "Bearer " + colaboradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Imprevisto\"}"))
                .andExpect(status().isCreated());

        String gerenteToken = loginAndGetToken("gerente", "87654321");
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalDate ontem = hoje.minusDays(1);

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2000-01-01")
                        .param("end_date", "2000-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(0))
                .andExpect(jsonPath("$.journeys_progress").value(0))
                .andExpect(jsonPath("$.average_adherence_percentage").value(0))
                .andExpect(jsonPath("$.activities_completed").value(0))
                .andExpect(jsonPath("$.unplanned_activities").value(0));

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", ontem.toString())
                        .param("end_date", ontem.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(0))
                .andExpect(jsonPath("$.journeys_progress").value(0))
                .andExpect(jsonPath("$.average_adherence_percentage").value(0))
                .andExpect(jsonPath("$.activities_completed").value(0))
                .andExpect(jsonPath("$.unplanned_activities").value(0));

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", hoje.toString())
                        .param("end_date", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(0))
                .andExpect(jsonPath("$.journeys_progress").value(1))
                .andExpect(jsonPath("$.average_adherence_percentage").value(50))
                .andExpect(jsonPath("$.activities_completed").value(1))
                .andExpect(jsonPath("$.unplanned_activities").value(1));
    }

    @Test
    @DirtiesContext
    void deveRetornarTotalHorasDoPeriodoSomandoJornadasFinalizadasPorDia() throws Exception {
        Colaborator colaborador = colaboratorRepository.findByUser_Username("colaborador")
                .orElseThrow();

        salvarJornadaFinalizada(colaborador, DIA_COM_DUAS_JORNADAS, LocalTime.of(8, 0), DURACAO_JORNADA_1_DIA_10);
        salvarJornadaFinalizada(colaborador, DIA_COM_DUAS_JORNADAS, LocalTime.of(14, 0), DURACAO_JORNADA_2_DIA_10);
        salvarJornadaFinalizada(colaborador, DIA_COM_UMA_JORNADA, LocalTime.of(9, 0), DURACAO_JORNADA_DIA_11);

        String gerenteToken = loginAndGetToken("gerente", "87654321");

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", DIA_COM_DUAS_JORNADAS.toString())
                        .param("end_date", DIA_COM_DUAS_JORNADAS.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(TOTAL_HORAS_DIA_10))
                .andExpect(jsonPath("$.journeys_progress").value(0));

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", DIA_COM_UMA_JORNADA.toString())
                        .param("end_date", DIA_COM_UMA_JORNADA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(DURACAO_JORNADA_DIA_11))
                .andExpect(jsonPath("$.journeys_progress").value(0));

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", DIA_COM_DUAS_JORNADAS.toString())
                        .param("end_date", DIA_COM_UMA_JORNADA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(TOTAL_HORAS_DIA_10 + DURACAO_JORNADA_DIA_11))
                .andExpect(jsonPath("$.journeys_progress").value(0));

        mockMvc.perform(get("/v1/manager/overview")
                        .header("Authorization", "Bearer " + gerenteToken)
                        .param("start_date", "2000-01-01")
                        .param("end_date", "2000-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(0));
    }

    private void salvarJornadaFinalizada(
            Colaborator colaborador,
            LocalDate dia,
            LocalTime horaInicio,
            long durationSeconds) {
        Instant startedAt = dia.atTime(horaInicio).atZone(APP_ZONE).toInstant();
        Instant endedAt = startedAt.plusSeconds(durationSeconds);
        journeyRepository.save(Journey.builder()
                .colaborator(colaborador)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSeconds(durationSeconds)
                .status(JourneyStatus.COMPLETED)
                .build());
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

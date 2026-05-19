package br.com.playercontabilidade.registroponto.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-rpa-import;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.rpa.api-key=test-rpa-key"
})
class RpaImportControllerTest {

    private static final String VALID_PAYLOAD = """
            {
              "source_system": "ponto_agil",
              "records": [
                {
                  "external_employee_id": "001",
                  "employee_name": "Natanael",
                  "work_date": "2026-05-18",
                  "check_in_at": "2026-05-18T08:00:00-03:00",
                  "check_out_at": "2026-05-18T17:00:00-03:00",
                  "raw_payload": { "portal_row": 1 }
                }
              ]
            }
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveImportarRegistrosComApiKeyValida() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "test-rpa-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported_count").value(1))
                .andExpect(jsonPath("$.ids", hasSize(1)));
    }

    @Test
    void deveRetornar401SemApiKey() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autorizado"));
    }

    @Test
    void deveRetornar401ComApiKeyInvalida() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "chave-errada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("API key RPA ausente ou inválida"));
    }

    @Test
    void deveRetornar400QuandoPayloadInvalido() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "test-rpa-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source_system": "ponto_agil",
                                  "records": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void deveRetornar400QuandoCheckOutAnteriorAoCheckIn() throws Exception {
        mockMvc.perform(post("/v1/rpa/imports")
                        .header("X-Rpa-Api-Key", "test-rpa-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source_system": "ponto_agil",
                                  "records": [
                                    {
                                      "employee_name": "Natanael",
                                      "work_date": "2026-05-18",
                                      "check_in_at": "2026-05-18T17:00:00-03:00",
                                      "check_out_at": "2026-05-18T08:00:00-03:00"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("check_out_at não pode ser anterior a check_in_at."));
    }
}

package com.jobtracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Regras de domínio contra PostgreSQL 16: exclusão lógica, imagem de perfil, isolamento entre
 * usuários e candidatura em etapa desativada. Todos os dados são fictícios e o banco é efêmero.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
class PostgresDomainIT {
    private static final String PASSWORD = "Password123!";
    private static final String APPLIED_AT = "2026-11-05";

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void softDeletedRecordsStayInPostgresButDisappearFromTheApi() throws Exception {
        String token = register(email("exclusao"));
        long companyId = createCompany(token);
        long stageId = createStage(token, "Inscrito");
        long applicationId = createApplication(token, companyId, stageId, "Desenvolvedor");
        request(
                post("/api/applications/" + applicationId + "/notes"),
                token,
                Map.of("content", "Nota de teste"),
                201);

        send(delete("/api/applications/" + applicationId), token, 204);
        request(get("/api/applications/" + applicationId), token, null, 404);
        assertEquals(0, request(get("/api/applications"), token, null, 200).size());
        assertEquals(
                1,
                count(
                        "select count(*) from job_applications where id = ? and deleted_at is not null",
                        applicationId));
        assertEquals(
                1,
                count(
                        "select count(*) from application_history where application_id = ?",
                        applicationId));
        assertEquals(
                1,
                count(
                        "select count(*) from application_notes where application_id = ?",
                        applicationId));

        send(delete("/api/companies/" + companyId), token, 204);
        request(get("/api/companies/" + companyId), token, null, 404);
        assertEquals(
                1,
                count(
                        "select count(*) from companies where id = ? and deleted_at is not null",
                        companyId));
    }

    @Test
    void profileImageIsStoredInPostgresAndReturnedAfterLogin() throws Exception {
        String email = email("perfil");
        String token = register(email);
        String image = pngImage(1024, 1024);

        assertEquals(
                image,
                request(put("/api/auth/me/profile-image"), token, Map.of("image", image), 200)
                        .get("profileImage")
                        .asText());
        assertEquals(
                image, request(get("/api/auth/me"), token, null, 200).get("profileImage").asText());
        JsonNode login =
                request(
                        post("/api/auth/login"),
                        null,
                        Map.of("email", email, "password", PASSWORD),
                        200);
        assertEquals(image, login.get("profileImage").asText());
        assertEquals(
                image,
                jdbc.queryForObject(
                        "select profile_image from users where email = ?", String.class, email));
    }

    @Test
    void usersCannotReadOrChangeEachOthersDataOnPostgres() throws Exception {
        String owner = register(email("dono"));
        String intruder = register(email("intruso"));
        long companyId = createCompany(owner);
        long stageId = createStage(owner, "Entrevista");
        long applicationId = createApplication(owner, companyId, stageId, "Vaga privada");
        String path = "/api/applications/" + applicationId;

        request(get(path), intruder, null, 403);
        request(
                put(path),
                intruder,
                application(companyId, stageId, "Alterada por outro usuario"),
                403);
        request(get("/api/companies/" + companyId), intruder, null, 404);
        request(
                post("/api/applications"),
                intruder,
                application(companyId, stageId, "Tentativa"),
                400);
        assertFalse(
                contains(request(get("/api/applications"), intruder, null, 200), applicationId));
        assertFalse(contains(request(get("/api/stages"), intruder, null, 200), stageId));
        assertEquals("Vaga privada", request(get(path), owner, null, 200).get("jobTitle").asText());
    }

    @Test
    void applicationInDeactivatedStageIsKeptAndBlocksNewUsageOnPostgres() throws Exception {
        String token = register(email("etapa"));
        long companyId = createCompany(token);
        long deactivatedStage = createStage(token, "Entrevista");
        long activeStage = createStage(token, "Aguardando retorno");
        long kept =
                createApplication(token, companyId, deactivatedStage, "Vaga com etapa desativada");
        long other = createApplication(token, companyId, activeStage, "Vaga na etapa ativa");

        send(patch("/api/stages/" + deactivatedStage + "/deactivate"), token, 204);

        assertEquals(
                "Entrevista",
                request(get("/api/applications/" + kept), token, null, 200)
                        .get("stageName")
                        .asText());
        assertTrue(contains(request(get("/api/applications"), token, null, 200), kept));
        // Editar sem trocar a etapa desativada continua permitido.
        request(
                put("/api/applications/" + kept),
                token,
                application(companyId, deactivatedStage, "Vaga editada"),
                200);
        // Nova candidatura e troca para a etapa desativada são recusadas.
        request(
                post("/api/applications"),
                token,
                application(companyId, deactivatedStage, "Nova candidatura"),
                400);
        request(
                patch("/api/applications/" + other + "/stage"),
                token,
                Map.of("stageId", deactivatedStage),
                400);

        // Registro do comportamento atual, com decisão de produto pendente: a candidatura
        // conta no total, mas a distribuição por etapa considera apenas etapas ativas.
        assertEquals(
                2,
                request(get("/api/dashboard/summary"), token, null, 200)
                        .get("totalApplications")
                        .asInt());
        int distributed = 0;
        for (JsonNode stage :
                request(get("/api/dashboard/status-distribution"), token, null, 200)) {
            distributed += stage.get("count").asInt();
        }
        assertEquals(1, distributed);
    }

    private String register(String email) throws Exception {
        return request(
                        post("/api/auth/register"),
                        null,
                        Map.of("name", "Usuario de teste", "email", email, "password", PASSWORD),
                        201)
                .get("token")
                .asText();
    }

    private long createCompany(String token) throws Exception {
        return request(
                        post("/api/companies"),
                        token,
                        Map.of("name", "Empresa " + UUID.randomUUID()),
                        201)
                .get("id")
                .asLong();
    }

    private long createStage(String token, String name) throws Exception {
        return request(
                        post("/api/stages"),
                        token,
                        Map.of("name", name, "position", 0, "color", "#2563eb", "isFinal", false),
                        201)
                .get("id")
                .asLong();
    }

    private long createApplication(String token, long companyId, long stageId, String title)
            throws Exception {
        return request(
                        post("/api/applications"),
                        token,
                        application(companyId, stageId, title),
                        201)
                .get("id")
                .asLong();
    }

    private Map<String, Object> application(long companyId, long stageId, String title) {
        return Map.of(
                "companyId", companyId,
                "stageId", stageId,
                "jobTitle", title,
                "appliedAt", APPLIED_AT);
    }

    private static String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private static boolean contains(JsonNode list, long id) {
        for (JsonNode item : list) {
            if (item.get("id").asLong() == id) return true;
        }
        return false;
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void send(MockHttpServletRequestBuilder builder, String token, int status)
            throws Exception {
        mvc.perform(builder.header("Authorization", "Bearer " + token))
                .andExpect(status().is(status));
    }

    private JsonNode request(
            MockHttpServletRequestBuilder builder, String token, Object body, int status)
            throws Exception {
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null)
            builder.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        return mapper.readTree(
                mvc.perform(builder)
                        .andExpect(status().is(status))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    private String pngImage(int width, int height) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }
}

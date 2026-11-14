package com.jobtracker;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class ApiRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void editsViewsAndSoftDeletesRecordsWhilePreservingHistoryAndOwnership() throws Exception {
        String token = register(UUID.randomUUID() + "@example.com");
        String outsider = register(UUID.randomUUID() + "@example.com");
        JsonNode company =
                request(post("/api/companies"), token, Map.of("name", "Original company"), 201);
        long companyId = company.get("id").asLong();
        String companyPath = "/api/companies/" + companyId;
        request(put(companyPath), outsider, Map.of("name", "Unauthorized"), 403);
        JsonNode editedCompany =
                request(
                        put(companyPath),
                        token,
                        Map.of(
                                "name",
                                "Updated company",
                                "cnpj",
                                "12345678000199",
                                "description",
                                "Company description"),
                        200);
        assertEquals("Updated company", editedCompany.get("name").asText());
        assertEquals(
                "Company description",
                request(get(companyPath), token, null, 200).get("description").asText());

        JsonNode stage =
                request(
                        post("/api/stages"),
                        token,
                        Map.of(
                                "name",
                                "Initial",
                                "position",
                                0,
                                "color",
                                "#2563eb",
                                "isFinal",
                                false),
                        201);
        long stageId = stage.get("id").asLong();
        String stagePath = "/api/stages/" + stageId;
        request(get(stagePath), outsider, null, 403);
        request(
                put(stagePath),
                token,
                Map.of("name", "Interview", "position", 0, "color", "#123456", "isFinal", true),
                200);
        assertEquals("Interview", request(get(stagePath), token, null, 200).get("name").asText());

        var payload =
                new java.util.HashMap<String, Object>(
                        Map.of(
                                "companyId",
                                companyId,
                                "stageId",
                                stageId,
                                "jobTitle",
                                "Developer",
                                "salary",
                                12500,
                                "jobUrl",
                                "https://example.com/job",
                                "description",
                                "Details",
                                "appliedAt",
                                "2026-11-13"));
        JsonNode app = request(post("/api/applications"), token, payload, 201);
        long appId = app.get("id").asLong();
        String appPath = "/api/applications/" + appId;
        request(post(appPath + "/notes"), token, Map.of("content", "Keep this note"), 201);
        payload.put("jobTitle", "Senior Developer");
        payload.put("salary", 15000);
        request(put(appPath), outsider, payload, 403);
        request(put(appPath), token, payload, 200);
        assertEquals(
                "Senior Developer",
                request(get(appPath), token, null, 200).get("jobTitle").asText());

        mvc.perform(delete(companyPath).header("Authorization", "Bearer " + outsider))
                .andExpect(status().isForbidden());
        mvc.perform(delete(stagePath).header("Authorization", "Bearer " + outsider))
                .andExpect(status().isForbidden());
        mvc.perform(delete(appPath).header("Authorization", "Bearer " + outsider))
                .andExpect(status().isForbidden());
        mvc.perform(delete(companyPath).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(delete(stagePath).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        request(get(companyPath), token, null, 404);
        request(get(stagePath), token, null, 404);
        request(put(companyPath), token, Map.of("name", "Deleted"), 404);
        request(
                put(stagePath),
                token,
                Map.of("name", "Deleted", "position", 0, "color", "blue"),
                404);
        for (JsonNode visible : request(get("/api/companies"), token, null, 200)) {
            assertNotEquals(companyId, visible.get("id").asLong());
        }
        assertEquals(0, request(get("/api/stages"), token, null, 200).size());
        request(post("/api/applications"), token, payload, 404);
        // Existing applications can still be viewed and edited with their historical links.
        assertEquals(
                "Updated company",
                request(get(appPath), token, null, 200).get("companyName").asText());
        assertEquals(15000, request(put(appPath), token, payload, 200).get("salary").asInt());

        mvc.perform(delete(appPath).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        request(get(appPath), token, null, 404);
        request(put(appPath), token, payload, 404);
        request(get(appPath + "/history"), token, null, 404);
        request(get(appPath + "/notes"), token, null, 404);
        request(post(appPath + "/notes"), token, Map.of("content", "Hidden"), 404);
        assertEquals(0, request(get("/api/applications"), token, null, 200).size());
        JsonNode summary = request(get("/api/dashboard/summary"), token, null, 200);
        assertEquals(0, summary.get("totalApplications").asInt());
        assertEquals(0, summary.get("finalApplications").asInt());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from job_applications where id = ? and deleted_at is not null",
                        Integer.class,
                        appId));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from companies where id = ? and deleted_at is not null",
                        Integer.class,
                        companyId));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from application_stages where id = ? and is_active = false",
                        Integer.class,
                        stageId));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from application_notes where application_id = ?",
                        Integer.class,
                        appId));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from application_history where application_id = ?",
                        Integer.class,
                        appId));
    }

    @Test
    void preservesRoutesPayloadsJwtAndOwnership() throws Exception {
        mvc.perform(get("/api/applications")).andExpect(status().isForbidden());
        mvc.perform(get("/api/applications").header("Authorization", "Bearer invalid"))
                .andExpect(status().isForbidden());

        String email = UUID.randomUUID() + "@example.com";
        String token = register(email);
        JsonNode login =
                request(
                        post("/api/auth/login"),
                        null,
                        Map.of("email", email, "password", "Password123!"),
                        200);
        mvc.perform(
                        get("/api/auth/me")
                                .header("Authorization", "Bearer " + login.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("email").value(email))
                .andExpect(jsonPath("role").value("USER"));
        request(
                post("/api/stages"),
                token,
                Map.of("name", "", "position", -1, "color", "blue"),
                400);
        JsonNode stage =
                request(
                        post("/api/stages"),
                        token,
                        Map.of("name", "Applied", "position", 0, "color", "blue", "isFinal", false),
                        201);
        JsonNode next =
                request(
                        post("/api/stages"),
                        token,
                        Map.of("name", "Closed", "position", 1, "color", "green", "isFinal", true),
                        201);
        JsonNode company =
                request(
                        post("/api/companies"),
                        token,
                        Map.of("name", "Company " + UUID.randomUUID()),
                        201);
        String outsider = register(UUID.randomUUID() + "@example.com");
        request(get("/api/companies/" + company.get("id")), outsider, null, 404);

        JsonNode app =
                request(
                        post("/api/applications"),
                        token,
                        Map.of(
                                "companyId",
                                company.get("id").asLong(),
                                "stageId",
                                stage.get("id").asLong(),
                                "jobTitle",
                                "Developer",
                                "appliedAt",
                                "2026-11-05"),
                        201);
        String path = "/api/applications/" + app.get("id");
        request(get(path), outsider, null, 403);
        request(get("/api/stages"), token, null, 200);
        request(get("/api/companies"), token, null, 200);
        request(get("/api/applications"), token, null, 200);
        request(patch(path + "/stage"), token, Map.of("stageId", next.get("id").asLong()), 200);
        mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("finalStage").value(true))
                .andExpect(jsonPath("jobTitle").value("Developer"));
        request(post(path + "/notes"), token, Map.of("content", "Interview complete"), 201);
        mvc.perform(get(path + "/notes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Interview complete"));
        mvc.perform(get(path + "/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalApplications").value(1))
                .andExpect(jsonPath("activeApplications").value(0))
                .andExpect(jsonPath("finalApplications").value(1));
        mvc.perform(
                        get("/api/dashboard/status-distribution")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].count").value(1));
    }

    @Test
    void preservesAdminAuthorizationAndCompanyReview() throws Exception {
        String token = register(UUID.randomUUID() + "@example.com");
        JsonNode company =
                request(
                        post("/api/companies"),
                        token,
                        Map.of("name", "Review " + UUID.randomUUID()),
                        201);
        request(get("/api/admin/companies/pending"), token, null, 403);
        String approval = "/api/admin/companies/" + company.get("id") + "/approve";
        request(patch(approval), token, null, 403);
        JsonNode login =
                request(
                        post("/api/auth/login"),
                        null,
                        Map.of("email", "admin@jobtracker.local", "password", "Admin123!"),
                        200);
        String admin = login.get("token").asText();
        request(get("/api/admin/companies/pending"), admin, null, 200);
        mvc.perform(patch(approval).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("approvalStatus").value("APPROVED"));
        request(
                get("/api/companies/" + company.get("id")),
                register(UUID.randomUUID() + "@example.com"),
                null,
                200);
    }

    private String register(String email) throws Exception {
        return request(
                        post("/api/auth/register"),
                        null,
                        Map.of("name", "Test User", "email", email, "password", "Password123!"),
                        201)
                .get("token")
                .asText();
    }

    private JsonNode request(
            MockHttpServletRequestBuilder request, String token, Object body, int status)
            throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null)
            request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        return mapper.readTree(
                mvc.perform(request)
                        .andExpect(status().is(status))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }
}

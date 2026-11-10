package com.jobtracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void preservesRoutesPayloadsJwtAndOwnership() throws Exception {
        mvc.perform(get("/api/applications")).andExpect(status().isForbidden());
        mvc.perform(get("/api/applications").header("Authorization", "Bearer invalid"))
                .andExpect(status().isForbidden());

        String email = UUID.randomUUID() + "@example.com";
        String token = register(email);
        JsonNode login = request(post("/api/auth/login"), null,
                Map.of("email", email, "password", "Password123!"), 200);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login.get("token").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value(email))
                .andExpect(jsonPath("role").value("USER"));
        request(post("/api/stages"), token, Map.of("name", "", "position", -1, "color", "blue"), 400);
        JsonNode stage = request(post("/api/stages"), token,
                Map.of("name", "Applied", "position", 0, "color", "blue", "isFinal", false), 201);
        JsonNode next = request(post("/api/stages"), token,
                Map.of("name", "Closed", "position", 1, "color", "green", "isFinal", true), 201);
        JsonNode company = request(post("/api/companies"), token,
                Map.of("name", "Company " + UUID.randomUUID()), 201);
        String outsider = register(UUID.randomUUID() + "@example.com");
        request(get("/api/companies/" + company.get("id")), outsider, null, 404);

        JsonNode app = request(post("/api/applications"), token,
                Map.of("companyId", company.get("id").asLong(), "stageId", stage.get("id").asLong(),
                        "jobTitle", "Developer", "appliedAt", "2026-11-05"), 201);
        String path = "/api/applications/" + app.get("id");
        request(get(path), outsider, null, 403);
        request(get("/api/stages"), token, null, 200);
        request(get("/api/companies"), token, null, 200);
        request(get("/api/applications"), token, null, 200);
        request(patch(path + "/stage"), token, Map.of("stageId", next.get("id").asLong()), 200);
        mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("finalStage").value(true))
                .andExpect(jsonPath("jobTitle").value("Developer"));
        request(post(path + "/notes"), token, Map.of("content", "Interview complete"), 201);
        mvc.perform(get(path + "/notes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("Interview complete"));
        mvc.perform(get(path + "/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("totalApplications").value(1))
                .andExpect(jsonPath("activeApplications").value(0))
                .andExpect(jsonPath("finalApplications").value(1));
        mvc.perform(get("/api/dashboard/status-distribution").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[1].count").value(1));
    }

    @Test
    void preservesAdminAuthorizationAndCompanyReview() throws Exception {
        String token = register(UUID.randomUUID() + "@example.com");
        JsonNode company = request(post("/api/companies"), token,
                Map.of("name", "Review " + UUID.randomUUID()), 201);
        request(get("/api/admin/companies/pending"), token, null, 403);
        String approval = "/api/admin/companies/" + company.get("id") + "/approve";
        request(patch(approval), token, null, 403);
        JsonNode login = request(post("/api/auth/login"), null,
                Map.of("email", "admin@jobtracker.local", "password", "Admin123!"), 200);
        String admin = login.get("token").asText();
        request(get("/api/admin/companies/pending"), admin, null, 200);
        mvc.perform(patch(approval).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("approvalStatus").value("APPROVED"));
        request(get("/api/companies/" + company.get("id")), register(UUID.randomUUID() + "@example.com"), null, 200);
    }

    private String register(String email) throws Exception {
        return request(post("/api/auth/register"), null,
                Map.of("name", "Test User", "email", email, "password", "Password123!"), 201)
                .get("token").asText();
    }

    private JsonNode request(MockHttpServletRequestBuilder request, String token, Object body, int status)
            throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        return mapper.readTree(mvc.perform(request).andExpect(status().is(status))
                .andReturn().getResponse().getContentAsString());
    }
}

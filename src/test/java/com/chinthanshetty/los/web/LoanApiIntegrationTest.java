package com.chinthanshetty.los.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack: JWT auth, Flyway schema on H2, resilience wrappers, and the REST contract. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LoanApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token(String user, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(user, password)))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String application(String name, String pan, int income, int amount, int tenure) {
        return """
                {"productType":"PERSONAL","applicantName":"%s","pan":"%s","monthlyIncome":%d,
                 "existingEmi":0,"requestedAmount":%d,"tenureMonths":%d}""".formatted(name, pan, income, amount, tenure);
    }

    @Test
    void approvesAndRecordsFullAuditTrail() throws Exception {
        String officer = token("officer", "officer123");

        MvcResult created = mvc.perform(post("/api/v1/applications").header("Authorization", officer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("Asha Rao", "ABCDE0480F", 90000, 300000, 24)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.creditScore").value(780))
                .andExpect(jsonPath("$.panMasked").value("XXXXXXX80F"))
                .andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/v1/applications/{id}/audit", id).header("Authorization", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].eventType").value("APPLICATION_SUBMITTED"))
                .andExpect(jsonPath("$[3].eventType").value("DECISION_MADE"));
    }

    @Test
    void bureauOutageIsReferredNotFailed() throws Exception {
        mvc.perform(post("/api/v1/applications").header("Authorization", token("officer", "officer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("Kiran Shah", "PQRST9999K", 90000, 300000, 24)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REFERRED"))
                .andExpect(jsonPath("$.reasons[0]").value(startsWith("BUREAU_UNAVAILABLE")));
    }

    @Test
    void watchlistNameIsReferredToCompliance() throws Exception {
        mvc.perform(post("/api/v1/applications").header("Authorization", token("officer", "officer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("  ravi   shellwala ", "UVWXY0500Z", 150000, 300000, 24)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REFERRED"))
                .andExpect(jsonPath("$.reasons[0]").value(startsWith("SANCTIONS_POSSIBLE_MATCH")));
    }

    @Test
    void idempotencyKeyReturnsOriginalApplication() throws Exception {
        String officer = token("officer", "officer123");
        String body = application("Meera Iyer", "LMNOP0450Q", 80000, 200000, 12);

        MvcResult first = mvc.perform(post("/api/v1/applications").header("Authorization", officer)
                        .header("Idempotency-Key", "retry-123").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode original = json.readTree(first.getResponse().getContentAsString());

        mvc.perform(post("/api/v1/applications").header("Authorization", officer)
                        .header("Idempotency-Key", "retry-123").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(original.get("id").asText()));
    }

    @Test
    void officerCannotChangePolicyButRiskAdminPublishesNewVersion() throws Exception {
        String rules = """
                {"minCreditScore":650,"referBand":20,"maxFoir":0.55,"minMonthlyIncome":20000,
                 "maxLoanAmount":2000000,"maxTenureMonths":60,"annualInterestRate":13.0}""";

        mvc.perform(put("/api/v1/policies/VEHICLE").header("Authorization", token("officer", "officer123"))
                        .contentType(MediaType.APPLICATION_JSON).content(rules))
                .andExpect(status().isForbidden());

        String risk = token("risk", "risk123");
        mvc.perform(put("/api/v1/policies/VEHICLE").header("Authorization", risk)
                        .contentType(MediaType.APPLICATION_JSON).content(rules))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.rules.minCreditScore").value(650));

        mvc.perform(get("/api/v1/policies/VEHICLE/history").header("Authorization", risk))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void institutionsCannotSeeEachOthersApplications() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/applications").header("Authorization", token("officer", "officer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("Ravi Nair", "ABCDE0500F", 90000, 300000, 24)))
                .andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/v1/applications/{id}", id).header("Authorization", token("coop-officer", "coop123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidInputAndMissingToken() throws Exception {
        mvc.perform(post("/api/v1/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(application("X", "ABCDE0480F", 1, 1, 1)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/applications").header("Authorization", token("officer", "officer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(application("Bad Pan", "12345", 90000, 300000, 24)))
                .andExpect(status().isBadRequest());
    }
}

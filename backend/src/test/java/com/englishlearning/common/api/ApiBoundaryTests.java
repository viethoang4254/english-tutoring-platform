package com.englishlearning.common.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false"
})
class ApiBoundaryTests {
    @Autowired JsonMapper mapper;
    @Autowired Validator validator;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                .setValidator(validator).build();
    }

    record FixtureRequest(@NotNull ApiId id, @NotNull @Valid ApiMoney money,
            @NotNull Instant occurredAt, @NotBlank String label, @Min(1) int count) {}

    // Only on the test classpath; never packaged as an application endpoint.
    @RestController
    static class FixtureController {
        @PostMapping("/api/v1/fixture")
        FixtureRequest echo(@Valid @RequestBody FixtureRequest request) { return request; }

        @GetMapping("/api/v1/fixture/failure")
        void failure() { throw new IllegalStateException("password=PRIVATE_SECRET"); }
    }

    private String valid() {
        return """
                {"id":"9223372036854775807","money":{"amount":"9999999999999.99","currency":"VND"},
                 "occurredAt":"2026-10-05T09:00:00Z","label":"test","count":1}
                """;
    }

    @Test
    void precisionAndTimeRoundTripThroughHttp() throws Exception {
        String response = mvc.perform(post("/api/v1/fixture")
                .contentType(MediaType.APPLICATION_JSON).content(valid()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var tree = mapper.readTree(response);
        assertTrue(tree.get("id").isString());
        assertEquals(Long.MAX_VALUE + "", tree.get("id").asString());
        assertEquals("9999999999999.99", tree.get("money").get("amount").asString());
        assertEquals("VND", tree.get("money").get("currency").asString());
        assertEquals("2026-10-05T09:00:00Z", tree.get("occurredAt").asString());
        assertEquals(1, tree.get("count").asInt());
        FixtureRequest decoded = mapper.readValue(response, FixtureRequest.class);
        assertEquals(Long.MAX_VALUE, decoded.id().value());
        assertEquals(new BigDecimal("9999999999999.99"), decoded.money().decimalAmount());
        assertEquals(Instant.parse("2026-10-05T09:00:00Z"), decoded.occurredAt());
        assertEquals("0.00", ApiMoney.from(new BigDecimal("0.00"), "VND").amount());
    }

    @Test
    void explicitOffsetIsAcceptedWithoutLosingInstant() throws Exception {
        var dto = mapper.readValue(valid().replace("09:00:00Z", "16:00:00+07:00"),
                FixtureRequest.class);
        assertEquals(Instant.parse("2026-10-05T09:00:00Z"), dto.occurredAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"role", "ownerId", "locked", "unknownProperty"})
    void unknownAndProtectedFieldsAreRejected(String field) throws Exception {
        reject(valid().replace("\"count\":1", "\"count\":1,\"" + field + "\":\"PRIVATE_SECRET\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{", "[]", "null",
            "{\"id\":123}", "{\"id\":\"9223372036854775808\"}",
            "{\"id\":\"1.5\"}", "{\"id\":\"NaN\"}"
    })
    void malformedAndInvalidIdentifiersAreRejected(String body) throws Exception { reject(body); }

    @Test
    void scalarCoercionAndNestedUnknownsAreRejected() throws Exception {
        reject(valid().replace("\"count\":1", "\"count\":\"1\""));
        reject(valid().replace("\"count\":1", "\"count\":1.5"));
        reject(valid().replace("\"count\":1", "\"count\":null"));
        reject(valid().replace("\"label\":\"test\"", "\"label\":123"));
        reject(valid().replace("\"id\":\"9223372036854775807\"", "\"id\":123"));
        reject(valid().replace("\"9999999999999.99\"", "999.99"));
        reject(valid().replace("\"9999999999999.99\"", "\"NaN\""));
        reject(valid().replace("\"currency\":\"VND\"", "\"currency\":\"VND\",\"secret\":\"PRIVATE_SECRET\""));
        reject(valid() + "{}");
        reject(valid().replace("\"2026-10-05T09:00:00Z\"", "1791190800"));
        reject(valid().replace("2026-10-05T09:00:00Z", "2026-10-05T09:00:00"));
    }

    @Test
    void validationUsesOptionalSafeFieldMap() throws Exception {
        String response = reject(valid().replace("\"label\":\"test\"", "\"label\":\"\""));
        assertEquals("Invalid value.", mapper.readTree(response).get("fieldErrors").get("label").asString());
        assertEquals(Map.of("label", "Invalid value."),
                mapper.convertValue(mapper.readTree(response).get("fieldErrors"), Map.class));
    }

    @Test
    void unexpectedFailuresNeverExposeInternalDetails() throws Exception {
        String response = mvc.perform(get("/api/v1/fixture/failure"))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();
        var tree = mapper.readTree(response);
        assertEquals("INTERNAL_ERROR", tree.get("code").asString());
        assertFalse(tree.has("fieldErrors"));
        assertFalse(response.contains("PRIVATE_SECRET"));
        assertFalse(response.contains("IllegalStateException"));
        assertFalse(response.contains("stackTrace"));
    }

    @Test
    void frameworkErrorsKeepStatusAndSafeBody() throws Exception {
        String response = mvc.perform(put("/api/v1/fixture"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andReturn().getResponse().getContentAsString();
        assertEquals("REQUEST_REJECTED", mapper.readTree(response).get("code").asString());
    }

    private String reject(String body) throws Exception {
        String response = mvc.perform(post("/api/v1/fixture")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertEquals("INVALID_REQUEST", mapper.readTree(response).get("code").asString());
        assertFalse(response.contains("PRIVATE_SECRET"));
        assertFalse(response.contains("Exception"));
        return response;
    }
}

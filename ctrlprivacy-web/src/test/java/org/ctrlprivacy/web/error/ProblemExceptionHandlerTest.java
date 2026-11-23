package org.ctrlprivacy.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ctrlprivacy.web.api.ApiResponse;
import org.ctrlprivacy.web.ip.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The error answers and the client IP resolver through a real Spring MVC application. Method
 * security denials are ctrlprivacy-security's, and tested there.
 */
@SpringBootTest(
        classes = ProblemExceptionHandlerTest.TestApplication.class,
        properties = {"ctrlprivacy.web.trusted-proxies=10.0.0.0/8", "logging.level.org.ctrlprivacy.web=OFF"})
@AutoConfigureMockMvc
class ProblemExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void wrapsSuccessInTheEnvelope() throws Exception {
        mvc.perform(get("/v1/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("fine"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void answersABusinessErrorWithItsStatusAndCode() throws Exception {
        mvc.perform(get("/v1/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("THING_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Thing not found: 42"));
    }

    @Test
    void answersAFailedValidationWithTheInvalidFields() throws Exception {
        mvc.perform(post("/v1/things").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void givesSpringMvcErrorsACodeToo() throws Exception {
        mvc.perform(post("/v1/things").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mvc.perform(get("/v1/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void answersAnUnexpectedFailureWithoutItsMessage() throws Exception {
        mvc.perform(get("/v1/broken"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An internal error occurred."));
    }

    @Test
    void configuresTheClientIpResolver() throws Exception {
        mvc.perform(get("/v1/ip").with(request -> {
                    request.setRemoteAddr("10.0.0.2");
                    request.addHeader(ClientIpResolver.X_FORWARDED_FOR, "198.51.100.1");
                    return request;
                }))
                .andExpect(jsonPath("$.data").value("198.51.100.1"));
    }

    @Getter
    @RequiredArgsConstructor
    enum TestError implements ErrorCode {
        THING_NOT_FOUND(HttpStatus.NOT_FOUND, "Thing not found.");

        private final HttpStatus status;
        private final String message;
    }

    /**
     * A request body with one required field.
     *
     * @param name must not be blank
     */
    record CreateThing(@NotBlank String name) {}

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        /**
         * The endpoints under test.
         *
         * @param clientIpResolver the auto-configured resolver
         * @return the controller
         */
        @Bean
        ThingController thingController(ClientIpResolver clientIpResolver) {
            return new ThingController(clientIpResolver);
        }
    }

    @RestController
    @RequiredArgsConstructor
    static class ThingController {

        private final ClientIpResolver clientIpResolver;

        /**
         * Succeeds.
         *
         * @return the envelope
         */
        @GetMapping("/v1/ok")
        ApiResponse<String> ok() {
            return ApiResponse.ok("fine");
        }

        /**
         * Throws a business error.
         *
         * @return never
         */
        @GetMapping("/v1/missing")
        ApiResponse<String> missing() {
            throw new CtrlPrivacyException(TestError.THING_NOT_FOUND, "Thing not found: 42");
        }

        /**
         * Validates its body.
         *
         * @param body the thing to create
         * @return the name
         */
        @PostMapping("/v1/things")
        ApiResponse<String> create(@Valid @RequestBody CreateThing body) {
            return ApiResponse.ok(body.name());
        }

        /**
         * Fails unexpectedly, with a message that must not reach the caller.
         *
         * @return never
         */
        @GetMapping("/v1/broken")
        ApiResponse<String> broken() {
            throw new IllegalStateException("database password is hunter2");
        }

        /**
         * The caller's address.
         *
         * @param request the current request
         * @return the resolved address
         */
        @GetMapping("/v1/ip")
        ApiResponse<String> ip(jakarta.servlet.http.HttpServletRequest request) {
            return ApiResponse.ok(clientIpResolver.resolve(request));
        }
    }
}

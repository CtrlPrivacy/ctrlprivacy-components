package org.ctrlprivacy.security.autoconfigure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.ctrlprivacy.security.jwt.AccessTokenDecoders;
import org.ctrlprivacy.security.testing.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@SpringBootTest(
        classes = SecurityTest.TestApplication.class,
        properties = "ctrlprivacy.security.public-paths=/v1/public/**")
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void answersAMissingTokenWith401AsAProblem() throws Exception {
        mvc.perform(get("/v1/things"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void answersAnInvalidTokenWith401() throws Exception {
        mvc.perform(get("/v1/things").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersAMissingPermissionWith403AsAProblem() throws Exception {
        mvc.perform(get("/v1/things").header(HttpHeaders.AUTHORIZATION, bearer(List.of("OTHER:READ"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void readsPermissionsAsAuthorities() throws Exception {
        mvc.perform(get("/v1/things").header(HttpHeaders.AUTHORIZATION, bearer(List.of("THING:READ"))))
                .andExpect(status().isOk())
                .andExpect(content().string("kratos-identity"));
    }

    @Test
    void opensPublicPathsWithoutAToken() throws Exception {
        mvc.perform(get("/v1/public/things")).andExpect(status().isOk());
    }

    @Test
    void answersAMethodSecurityDenialOnAPublicPathAs401Or403DespiteACatchAll() throws Exception {
        mvc.perform(get("/v1/public/secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/v1/public/secret").header(HttpHeaders.AUTHORIZATION, bearer(List.of("OTHER:READ"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deniesEverythingOutsideTheApiEvenWithAToken() throws Exception {
        mvc.perform(get("/other").header(HttpHeaders.AUTHORIZATION, bearer(List.of("THING:READ"))))
                .andExpect(status().isForbidden());
    }

    /**
     * An {@code Authorization} header value with a valid test token.
     *
     * @param permissions the token's permissions
     * @return {@code Bearer <token>}
     */
    private static String bearer(List<String> permissions) {
        return "Bearer " + TestTokens.token(permissions);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        /**
         * Verifies the test tokens instead of fetching Hydra's JWKS.
         *
         * @return the decoder
         */
        @Bean
        JwtDecoder jwtDecoder() {
            return AccessTokenDecoders.create(TestTokens.publicKey(), TestTokens.ISSUER, TestTokens.AUDIENCE);
        }

        /**
         * The endpoints under test.
         *
         * @return the controller
         */
        @Bean
        ThingController thingController() {
            return new ThingController();
        }

        /**
         * A catch-all, as a service's error handling has, which a denial must never reach.
         *
         * @return the advice
         */
        @Bean
        CatchAll catchAll() {
            return new CatchAll();
        }
    }

    @RestControllerAdvice
    static class CatchAll {

        /**
         * Answers anything as 500.
         *
         * @param exception the failure
         * @return a fixed body
         */
        @ExceptionHandler(Exception.class)
        ResponseEntity<String> handle(Exception exception) {
            return ResponseEntity.internalServerError().body("caught");
        }
    }

    @RestController
    static class ThingController {

        /**
         * Needs {@code THING:READ}.
         *
         * @param authentication the caller
         * @return the caller's subject
         */
        @GetMapping("/v1/things")
        @PreAuthorize("hasAuthority('THING:READ')")
        String things(Authentication authentication) {
            return authentication.getName();
        }

        /**
         * Open without a token.
         *
         * @return a fixed body
         */
        @GetMapping("/v1/public/things")
        String publicThings() {
            return "public";
        }

        /**
         * Open in the filter chain, but needs {@code THING:READ}.
         *
         * @return a fixed body
         */
        @GetMapping("/v1/public/secret")
        @PreAuthorize("hasAuthority('THING:READ')")
        String secret() {
            return "secret";
        }

        /**
         * Outside {@code /v1}, so the chain denies it before it runs.
         *
         * @return a fixed body
         */
        @GetMapping("/other")
        String other() {
            return "other";
        }
    }
}

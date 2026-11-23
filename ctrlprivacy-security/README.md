# ctrlprivacy-security

## At a glance

| | |
|---|---|
| Artifact | `org.ctrlprivacy:ctrlprivacy-security` |
| Package | `org.ctrlprivacy.security` |
| Status | Experimental |
| Compatibility | Java 25, Spring Boot 4.2 |
| Auto-configuration | `SecurityAutoConfiguration`, `SecurityFilterChainAutoConfiguration`, `MethodSecurityAutoConfiguration` |
| Depends on | Spring Security, OAuth2 Resource Server |

## Purpose

The resource-server side of Ctrl Privacy's auth: a service verifies the access tokens Hydra issues
and decides what the caller may do from the `permissions` they carry, so every API enforces access
the same way. Signing in (Kratos), issuing tokens (Hydra) and deciding which permissions a user has
(auth-service's token hook) all happen elsewhere.

## Responsibilities

**Does**

- Verify Hydra's RS256 JWT access tokens: signature against Hydra's JWKS, lifetime, issuer and
  this API's audience.
- Turn the `permissions` claim into authorities and the `sub` claim into the principal name.
- Secure every route by default: public paths open, `/v1/**` authenticated, everything else denied.
- Answer 401 and 403 as `application/problem+json`.
- Turn on method security (`@PreAuthorize`, `@PermitAll`), and hand a denial thrown in a
  controller back to Spring Security (`SecurityExceptionPassthrough`), so it's answered 401 or 403
  rather than by a service's error handling.
- Say who the caller is: `Caller.currentUserKey()` reads the user's key from the token's `sub`, so
  no other module reads the security context or token claims.

**Does not**

- Sign anyone in or handle the browser session (Kratos and the gateway).
- Issue tokens or hold signing keys (Hydra).
- Store users, roles or permissions, or decide which permissions a user has (auth-service).

## Requirements

### Functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| SECURITY-FR-001 | A token must be accepted only when it is RS256-signed by Hydra's key, within its lifetime, from the configured issuer, and for this API's audience. | Engineering | `AccessTokenDecodersTest` |
| SECURITY-FR-002 | A missing or invalid token must be answered 401 with `WWW-Authenticate: Bearer`, and a missing authority 403, both as problem details. | Engineering | `SecurityTest` |
| SECURITY-FR-003 | Each entry of the `permissions` claim must become an authority with no prefix, and `sub` the principal name. | Engineering | `SecurityTest` |
| SECURITY-FR-004 | `ctrlprivacy.security.public-paths` must need no token, and every path outside `/v1/**` must be denied. | Engineering | `SecurityTest` |
| SECURITY-FR-005 | The actuator endpoints must answer without a token when the service has the actuator. | Engineering | Not yet verified |
| SECURITY-FR-006 | A method security denial in a controller must be answered 401 without a token and 403 without the permission, even with a catch-all exception handler in the service. | Engineering | `SecurityTest` |
| SECURITY-FR-007 | `Caller.currentUserKey()` must answer the token's `sub` as a UUID, and nothing without an authenticated caller or when `sub` isn't a UUID. | Engineering | `CallerTest` |

### Non-functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| SECURITY-NFR-001 | Hydra's JWKS must be fetched once and cached, not on every request. | Engineering | Not yet verified (Nimbus's JWK source caches by default) |

## Contract

Every service relies on these. Changing them is a breaking change.

- The `ctrlprivacy.security.*` properties under Configuration.
- The `permissions` claim as a JSON array of permission codes, and the authorities it produces
  (`TokenContract`).
- The `sub` claim as the caller's user key in auth-service (a UUID), and `Caller.currentUserKey()`
  reading it; ctrlprivacy-core records it as `createdBy` and `updatedBy`.
- The default routes: public paths, `/v1/**` authenticated, everything else denied; and the main
  chain's bean name, `ctrlprivacySecurityFilterChain`, and order, `SecurityFilterChainAutoConfiguration.ORDER`.
- The problem detail shape of 401 and 403 answers, with `code` `UNAUTHORIZED` or `FORBIDDEN`.

## Installation

```kotlin
dependencies {
    implementation("org.ctrlprivacy:ctrlprivacy-security")   // version from ctrlprivacy-dependencies
}
```

It brings in Spring Security and the OAuth2 resource server, and turns on the decoder and the
default filter chain once `ctrlprivacy.security.jwk-set-uri` is set.

## Configuration

| Property | Default | Description |
|---|---|---|
| `ctrlprivacy.security.issuer` | None | Hydra's issuer URL, exactly as tokens carry it in `iss`. Required with `jwk-set-uri`. |
| `ctrlprivacy.security.jwk-set-uri` | None | Hydra's JWKS, ideally its in-cluster address. Turns verification on. |
| `ctrlprivacy.security.audience` | None | This API's audience, such as `ctrlprivacy-activity`. Required with `jwk-set-uri`. |
| `ctrlprivacy.security.public-paths` | Empty | Request matcher patterns open without a token, such as `/v1/connection/**`. |

## Usage

```yaml
ctrlprivacy:
  security:
    issuer: ${HYDRA_ISSUER:http://localhost:4444/}
    jwk-set-uri: ${HYDRA_JWKS_URI:http://localhost:4444/.well-known/jwks.json}
    audience: ctrlprivacy-resource
    public-paths:
      - /v1/groups/search
```

```java
@PostMapping
@PreAuthorize("hasAuthority('RESOURCE:WRITE')")
ResponseEntity<GroupResponse> create(@Valid @RequestBody CreateGroupRequest request) { … }
```

### Reading the caller's permissions

Check permissions with `hasAuthority(...)`, but don't list the authentication's authorities to
find out what a caller holds: Spring Security 7 adds authorities of its own, such as
`FACTOR_BEARER`, to record how the caller authenticated. Read the token's `permissions` claim
instead (`jwt.getClaimAsStringList(TokenContract.PERMISSIONS)`).

### A route with its own credential

A webhook that authenticates another way (auth-service's Hydra token hook, with a shared secret)
gets its own `SecurityFilterChain` bean with a `securityMatcher` for its path and an `@Order`
below `SecurityFilterChainAutoConfiguration.ORDER`, so it runs before the main chain.

### Hydra's side

Hydra puts custom claims under `ext` unless they're promoted. Configure it with
`oauth2.allowed_top_level_claims: [permissions]` and `oauth2.mirror_top_level_claims: false`, so
`permissions` is a top-level claim, and grant each API's audience at consent.

## Security

- **Threats:** forged or replayed tokens; a token for one API used against another; an ID token
  passed as an access token; a route left open by mistake.
- **Untrusted input:** the `Authorization` header. Only a token that passes every check becomes an
  authentication; anything else is a 401.
- **Defaults:** every route outside the public paths needs a token, everything outside `/v1/**` is
  denied, and only RS256 is accepted.
- **Protections:** signature, lifetime, issuer and audience checks; the audience check also rejects
  Hydra's ID tokens, whose `aud` is a client id. Limits: the `typ` header isn't checked; a token
  stays valid until it expires, since there's no introspection or revocation check, so keep
  Hydra's access token lifespan short.
- **Reporting:** report a suspected vulnerability privately to the maintainers, not in a public
  issue.

## Testing

```bash
./gradlew :ctrlprivacy-security:test
```

The module publishes test fixtures for services. `TestTokens` signs Hydra-shaped tokens with a
throwaway RSA key, and `AccessTokenDecoders.create(TestTokens.publicKey(), TestTokens.ISSUER,
audience)` builds the `JwtDecoder` bean that verifies them, so a service's tests exercise real
tokens and its real filter chain without Hydra:

```kotlin
testImplementation(testFixtures("org.ctrlprivacy:ctrlprivacy-security"))
```

Nothing needs Docker.

## Standards and references

- [RFC 6750: Bearer token usage](https://www.rfc-editor.org/rfc/rfc6750)
- [RFC 9457: Problem details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [Spring Security OAuth2 resource server (JWT)](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Ory Hydra JWT access tokens](https://www.ory.com/docs/oauth2-oidc/jwt-access-token)

## Status

**Experimental.** Pre-1.0 (`0.1.0-SNAPSHOT`); auth-service is its first user, and
activity-service and resource-service follow in step 4 of the auth plan.

## Changelog

See the [ctrlprivacy-components releases](https://github.com/CtrlPrivacy/ctrlprivacy-components/releases).
Anything in Contract or Configuration is breaking.

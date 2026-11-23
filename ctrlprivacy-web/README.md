# ctrlprivacy-web

## At a glance

| | |
|---|---|
| Artifact | `org.ctrlprivacy:ctrlprivacy-web` |
| Package | `org.ctrlprivacy.web` |
| Status | Experimental |
| Compatibility | Java 25, Spring Boot 4.2 |
| Auto-configuration | `WebAutoConfiguration` |
| Depends on | Spring Web; Spring MVC and a servlet container from the service |

## Purpose

The web layer every Ctrl Privacy API does the same way: errors answered as problem details with a
stable `code`, the `ApiResponse` envelope for successful answers, and finding the client's address
behind the gateway and the cluster's proxies. Each service used to carry its own copy of these.

## Responsibilities

**Does**

- Answer a `CtrlPrivacyException` as `application/problem+json` with its error's status and `code`.
- Answer a failed `@Valid` as 400 `VALIDATION_FAILED`, listing each invalid field in `errors`.
- Give Spring MVC's own errors (unknown route, malformed body, wrong method) a `code`: the status's
  name, such as `NOT_FOUND`.
- Answer anything unexpected as 500 `INTERNAL_ERROR`, logged in full but never explained.
- Wrap successful answers in `ApiResponse` (`data`, `message`, `timestamp`).
- Resolve the client's IP from `X-Forwarded-For`, believing only trusted proxies.

**Does not**

- Anything about who the caller is or what they may do (`ctrlprivacy-security`): verifying
  tokens, the 401 and 403 answers, and method security denials, which ctrlprivacy-security hands
  back to Spring Security before this module's handler sees them. This module never references
  Spring Security.
- Rate limit (the gateway, and `activity-service` for its own endpoints).
- Configure CORS or `Origin` checks (the gateway).

## Requirements

### Functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| WEB-FR-001 | A `CtrlPrivacyException` must be answered as a problem detail with its error's status, its name as `code`, and the exception's message as `detail`. | Engineering | `ProblemExceptionHandlerTest` |
| WEB-FR-002 | A failed `@Valid` must be answered 400 with `code` `VALIDATION_FAILED` and each invalid field and its message in `errors`. | Engineering | `ProblemExceptionHandlerTest` |
| WEB-FR-003 | Every problem Spring MVC answers itself must carry a `code`, the status's name. | Engineering | `ProblemExceptionHandlerTest` |
| WEB-FR-004 | An unexpected exception must be answered 500 `INTERNAL_ERROR` without its message. | Engineering | `ProblemExceptionHandlerTest` |
| ~~WEB-FR-005~~ | ~~A method security denial must be answered by Spring Security: 401 without a token, 403 without the permission.~~ Moved to ctrlprivacy-security (SECURITY-FR-006). | Engineering | Retired |
| WEB-FR-006 | `X-Forwarded-For` must be believed only from a trusted proxy, walked from the right, and the first untrusted address taken as the client. | Engineering | `ClientIpResolverTest` |
| WEB-FR-007 | A trusted proxy entry that isn't an IP literal or CIDR block must fail startup, and no entry or header value may cause a DNS lookup. | Engineering | `TrustedProxiesTest` |

### Non-functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| WEB-NFR-001 | Resolving a client's address must not do any I/O. | Engineering | `TrustedProxiesTest` (literals only) |

## Contract

Services rely on these. Changing them is a breaking change.

- The problem shape: `status`, `title` (the reason phrase), `detail`, and `code`; `errors` as a list
  of `{field, message}` on `VALIDATION_FAILED`. It matches `ctrlprivacy-security`'s 401 and 403.
- The codes `VALIDATION_FAILED` and `INTERNAL_ERROR`, and the status names for Spring MVC's errors.
- `ErrorCode` (`name()`, `getStatus()`, `getMessage()`) and `CtrlPrivacyException`'s constructors.
- `ApiResponse`'s fields: `data`, `message`, `timestamp`.
- `ClientIpResolver.resolve` and the `ctrlprivacy.web.trusted-proxies` property.

## Installation

```kotlin
dependencies {
    implementation("org.ctrlprivacy:ctrlprivacy-web")   // version from ctrlprivacy-dependencies
}
```

In a servlet application it registers the exception handler and the `ClientIpResolver` bean. `@WebMvcTest` slices get it too. A
service's `core` module can depend on it just to throw `CtrlPrivacyException`; nothing is set up
without Spring MVC.

## Configuration

| Property | Default | Description |
|---|---|---|
| `ctrlprivacy.web.trusted-proxies` | Empty | Addresses and CIDR blocks of the proxies allowed to report the client in `X-Forwarded-For`. Empty, the header is never believed. |

## Usage

List the service's errors as an enum and throw them:

```java
@Getter
@RequiredArgsConstructor
public enum ResourceError implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found."),
    DUPLICATE(HttpStatus.CONFLICT, "Already exists.");

    private final HttpStatus status;
    private final String message;
}

throw new CtrlPrivacyException(ResourceError.NOT_FOUND, "ResourceGroup not found: " + key);
```

```json
{"type":"about:blank","title":"Not Found","status":404,
 "detail":"ResourceGroup not found: 0b6…","code":"NOT_FOUND"}
```

Wrap successful answers:

```java
@GetMapping("/{key}")
ApiResponse<GroupResponse> get(@PathVariable UUID key) {
    return ApiResponse.ok(groupService.getGroup(key));
}
```

Find the client:

```java
String ip = clientIpResolver.resolve(request);
```

### An exception answered differently

A service's own `@RestControllerAdvice` with an `@Order` earlier than `Ordered.LOWEST_PRECEDENCE`
runs before the shared handler. Give it no catch-all, or the shared one never runs. A service that
still has a handler extending `ResponseEntityExceptionHandler` keeps it, and the shared one stands
aside.

## Security

- **Threats:** a caller forging `X-Forwarded-For` to dodge rate limits or skew geolocation; error
  answers leaking internals (stack traces, queries, secrets); a misconfigured proxy list trusting
  the internet.
- **Untrusted input:** `X-Forwarded-For` and the socket peer, parsed as IP literals only; an entry
  that isn't one stops the walk. Exception messages, which reach the caller for business errors.
- **Defaults:** no proxy is trusted, so the header is ignored until configured. Unexpected
  exceptions are answered without their message.
- **Protections:** the header is walked from the right, so anything the client wrote itself is
  never used; trusted proxy entries are validated at startup; no DNS lookups. Limits: only
  `X-Forwarded-For` is read, not RFC 7239 `Forwarded`; a `CtrlPrivacyException`'s message is
  answered as is, so it must not hold anything private.
- **Reporting:** report a suspected vulnerability privately to the maintainers, not in a public
  issue.

## Testing

```bash
./gradlew :ctrlprivacy-web:test
```

`ProblemExceptionHandlerTest` runs a small Spring MVC application. Nothing needs Docker.

## Standards and references

- [RFC 9457: Problem details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [MDN: X-Forwarded-For](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Forwarded-For)
- [Spring MVC error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

## Status

**Experimental.** Pre-1.0 (`0.1.0-SNAPSHOT`); resource-service, activity-service and
auth-service all use it for error answers, and activity-service for client IP resolution.

## Changelog

See the [ctrlprivacy-components releases](https://github.com/CtrlPrivacy/ctrlprivacy-components/releases).
Anything in Contract or Configuration is breaking.

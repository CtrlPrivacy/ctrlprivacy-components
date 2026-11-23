# ctrlprivacy-core

## At a glance

| | |
|---|---|
| Artifact | `org.ctrlprivacy:ctrlprivacy-core` |
| Package | `org.ctrlprivacy.core` |
| Status | Experimental |
| Compatibility | Java 25, Spring Boot 4.2 |
| Auto-configuration | `AuditingAutoConfiguration` |
| Depends on | Spring Data MongoDB, MapStruct; ctrlprivacy-security optional (for the auditor) |

## Purpose

The data model every Ctrl Privacy service shares: each MongoDB entity extends `BaseEntity`, so every
document has the same identity (`_id`, `_key`) and audit trail (`createdAt`, `createdBy`,
`updatedAt`, `updatedBy`), recorded the same way, and each response extends `BaseResponse`, so the
API answers the same three of them (`key`, `createdAt`, `updatedAt`) and never the rest.

## Responsibilities

**Does**

- Give every entity `_id`, a generated and uniquely indexed `_key`, and the audit trail.
- Turn on Spring Data MongoDB auditing, recording the caller's user key (from
  ctrlprivacy-security's `Caller`, the token's `sub`) or the system UUID.
- Stamp field-level updates (`updateFirst`, `updateMulti`), which auditing doesn't see.
- Keep the base fields out of request-to-entity mappings (`EntityMapperConfig`).
- Define the response base: the key and timestamps, never `_id` or who made a change.

**Does not**

- Verify tokens or read who the caller is (`ctrlprivacy-security`, whose `Caller` the auditor
  asks).
- Answer errors or wrap responses (`ctrlprivacy-web`).
- Hold any service's entities, collections or business rules.

## Requirements

### Functional

| ID | Requirement | Owner | Verified by |
|---|---|---|---|
| CORE-FR-001 | A built entity must have a generated key, stored as `_key`, never as `key`. | Engineering | `BaseEntityTest` |
| CORE-FR-002 | Saving a new entity must set `createdAt`, `createdBy`, `updatedAt` and `updatedBy`; saving it again must change only the `updated` pair. | Engineering | `BaseEntityTest` |
| CORE-FR-003 | Mutable and immutable (`@Value`) entities must both read back with their base fields and be audited. | Engineering | `BaseEntityTest` |
| CORE-FR-004 | The auditor must be the caller's user key from `Caller.currentUserKey()` (the token's `sub`), or `Auditors.SYSTEM` (the nil UUID) without a caller, for an anonymous one, or for a subject that isn't a UUID. | Engineering | `AuditTest` |
| CORE-FR-005 | A mapper using `EntityMapperConfig` must never map a base field from a request, and a response must carry only the key and timestamps of them. | Engineering | `EntityMapperConfigTest` |
| CORE-FR-006 | `AuditStamp` must add `updatedAt` and `updatedBy` to a field-level update, and never the `created` pair. | Engineering | `AuditTest` |

### Non-functional

None.

## Contract

Every service relies on these. Changing them is a breaking change.

- `BaseEntity`'s properties and stored fields: `_id`, `_key`, `createdAt`, `createdBy`, `updatedAt`,
  `updatedBy` (UUIDs), and `EntityFields`' names for them.
- `BaseResponse`'s fields: `key`, `createdAt`, `updatedAt`.
- The auditor's values: UUIDs, the caller's user key (`sub`), or `Auditors.SYSTEM`
  (`00000000-0000-0000-0000-000000000000`).
- `EntityMapperConfig`, `AuditStamp.touch`, and the bean names in `AuditingAutoConfiguration`.

## Installation

```kotlin
dependencies {
    implementation("org.ctrlprivacy:ctrlprivacy-core")   // version from ctrlprivacy-dependencies
}
```

It turns on Spring Data MongoDB auditing (with `@DataMongoTest` slices too) and registers
`AuditStamp`. Don't add `@EnableMongoAuditing` in the service as well.

## Configuration

None. Timestamps come from the application's `Clock` bean when it defines one, otherwise the
system clock in UTC.

## Usage

A mutable entity:

```java
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Document(collection = "ResourceGroup")
public class ResourceGroupEntity extends BaseEntity {
    private String title;
}
```

An immutable one, whose own constructor Spring Data uses:

```java
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_ = @PersistenceCreator)
@Document(collection = "Scenario")
public class ScenarioEntity extends BaseEntity {
    String name;
}
```

Its response and mapper:

```java
@Getter
@SuperBuilder
@NoArgsConstructor
public class ResourceGroupResponse extends BaseResponse {
    private String title;
}

@Mapper(config = EntityMapperConfig.class)
public interface ResourceGroupMapper {
    ResourceGroupEntity toEntity(CreateResourceGroupRequest request);
    ResourceGroupResponse toResponse(ResourceGroupEntity entity);
}
```

A field-level update:

```java
mongoTemplate.updateFirst(query, auditStamp.touch(new Update().set("title", title)), ResourceGroupEntity.class);
```

Run queries and updates with the entity class, so the property `key` maps to the stored `_key`.

## Security

- **Threats:** a request setting an entity's key or audit trail; the API leaking internal ids or
  who made a change.
- **Untrusted input:** request bodies, which `EntityMapperConfig` keeps away from the base fields.
- **Defaults:** every save is audited; anything without an authenticated caller is recorded as
  the system UUID.
- **Protections:** responses have no field for `_id`, `createdBy` or `updatedBy`. Limits: a
  service can still map them by hand, and the audit trail records only the last change, not a
  history.
- **Reporting:** report a suspected vulnerability privately to the maintainers, not in a public
  issue.

## Testing

```bash
./gradlew :ctrlprivacy-core:test
```

The tests run Spring Data's real mapping, conversion and auditing against both entity styles,
without a database. Nothing needs Docker.

## Standards and references

- [Spring Data MongoDB auditing](https://docs.spring.io/spring-data/mongodb/reference/mongodb/auditing.html)
- [Lombok @SuperBuilder](https://projectlombok.org/features/experimental/SuperBuilder)
- [MapStruct shared configurations](https://mapstruct.org/documentation/stable/reference/html/#shared-configurations)

## Status

**Experimental.** Pre-1.0 (`0.1.0-SNAPSHOT`); resource-service, activity-service and auth-service
adopt it together.

## Changelog

See the [ctrlprivacy-components releases](https://github.com/CtrlPrivacy/ctrlprivacy-components/releases).
Anything in Contract or Configuration is breaking.

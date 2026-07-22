# Jackson 3 Migration Plan

**Status:** Planned follow-up; do not combine with the Spring Boot 4 provider repair.

## Goal

Remove the temporary `spring-boot-jackson2` compatibility bridge and make the backend use Jackson 3 end to end without changing its public JSON contracts.

## Preconditions

- The provider-repair branch is merged and its contract tests are green.
- A representative production JSONB payload and JWT error response are captured as test fixtures.
- All dependencies that expose Jackson 2 types have an available Jackson 3-compatible version or a documented replacement.

## Sprint 1 — inventory and contract baseline

1. Generate an import and dependency inventory for `com.fasterxml.jackson.*`, including transitive users such as JJWT, SpringDoc, MyBatis handlers, Redisson, and Hutool.
2. Add black-box MVC tests for representative request/response payloads, validation errors, JWT authentication failures, and OpenAPI JSON.
3. Add PostgreSQL JSONB round-trip tests for every custom type handler and persisted AI/ontology payload.
4. Record JSON snapshots only for stable public fields; do not assert serialization order unless the API promises it.

**Exit criterion:** baseline tests run on the current Jackson 2 bridge and identify every direct import.

## Sprint 2 — dependency compatibility

1. Upgrade or replace libraries that block Jackson 3; do not shade or dual-load Jackson major versions.
2. Migrate direct imports and extension points to Jackson 3 APIs in small, independently testable batches.
3. Replace deprecated `ObjectMapper` customization patterns with Spring Boot 4/Jackson 3 supported configuration.
4. Keep `spring-boot-jackson2` enabled until no compiled production or test code requires it.

**Exit criterion:** all modules compile with Jackson 3 APIs while the bridge remains only as a temporary transitive safety net.

## Sprint 3 — bridge removal and rollout

1. Remove direct Jackson 2 dependencies and `spring-boot-jackson2` from the backend POM.
2. Run the MVC, JWT, JSONB, OpenAPI, provider-contract, and full application-context suites against PostgreSQL, Redis, and Neo4j.
3. Compare stable JSON fixtures with the baseline, explicitly review changes in date/time, null handling, enum formatting, and polymorphic payloads.
4. Release behind a canary deployment; monitor serialization errors and database read failures before broad rollout.

**Exit criterion:** no Jackson 2 artifacts appear in the runtime dependency tree and all public-contract tests pass.

## Risks and rollback

- Jackson module ecosystem compatibility is the primary risk, especially JJWT and database type handlers.
- A serialization change can corrupt interoperability without causing compile failures; contract tests are mandatory.
- Roll back by restoring the bridge and the last known-good dependency lock/commit. Do not attempt live JSONB rewrites without a separate data-migration plan.

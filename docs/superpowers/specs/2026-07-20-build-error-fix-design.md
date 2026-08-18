# Maven build error fix design

## Goal

Restore successful Maven compilation and testing without changing the existing API response contract.

## Root causes

1. `UserServiceImpl` defines business error constants as `String`, while `BusinessException`, `GlobalExceptionHandler`, and `Result` consistently use an integer `code`.
2. A duplicate `AgentBackendApplication` source file exists under `src/test/java`, which would conflict with the production application class during test compilation.
3. `UserMapperTest` was previously under `src/main/java`; it has already been moved correctly to `src/test/java` by the user.
4. Full test execution revealed that MyBatis Spring Boot Starter 3.0.5 supports Spring Boot 3.2-3.5, while this project uses Spring Boot 4.1.0. Its auto-configuration therefore evaluated before the Spring Boot 4 data source was available and did not register `UserMapper`.
5. Database settings exist only in `application-dev.yml`, but `UserMapperTest` did not activate the `dev` profile, so Spring could not see the JDBC URL or driver setting.

## Chosen design

- Preserve the existing integer API response contract.
- Replace the four string constants in `UserServiceImpl` with distinct integer business codes:
  - parameter error: `40001`
  - user not found: `40401`
  - duplicate user: `40901`
  - user creation failure: `50001`
- Remove only the duplicate test-side `AgentBackendApplication.java`.
- Keep `UserMapperTest` in `src/test/java` unchanged.
- Upgrade `mybatis-spring-boot-starter` from `3.0.5` to the Spring Boot 4-compatible `4.0.1` release.
- Add `@ActiveProfiles("dev")` to the mapper integration test so it loads the intended local database configuration without making `dev` the global default profile.

## Verification

1. Confirm the current build fails with the eight expected `String`-to-`int` errors.
2. Apply only the two changes above.
3. Run `mvn clean compile`.
4. Run `mvn test` with the configured MySQL password supplied only to the Maven process.
5. Confirm compilation succeeds and the mapper integration test passes.

## Non-goals

- Do not change `BusinessException`, `Result`, controllers, database schema, or endpoint behavior.
- Do not refactor unrelated source files.

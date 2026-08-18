# Maven Build Error Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the project compile and test successfully while preserving the existing integer API response contract.

**Architecture:** Keep `Result.code` and `BusinessException.code` as integers and align the service-layer constants with that contract. Keep tests only in Maven's test source set and remove the duplicate test-side Spring Boot application class.

**Tech Stack:** Java 26, Spring Boot 4.1.0, Maven, JUnit 5, MyBatis, MySQL 8.4.

## Global Constraints

- Preserve the existing integer API response contract.
- Use business codes `40001`, `40401`, `40901`, and `50001` exactly.
- Use MyBatis Spring Boot Starter `4.0.1`, which supports Spring Boot 4.
- Do not change controllers, database schema, mapper behavior, or endpoint behavior.
- Do not persist the MySQL password in source files, Maven configuration, or shell environment settings.
- The directory is not a Git repository, so commit steps are intentionally omitted.

---

### Task 1: Align service exception codes with the API contract

**Files:**
- Modify: `src/main/java/com/ljl/agent/service/impl/UserServiceImpl.java:23`
- Test: Maven main-source compilation

**Interfaces:**
- Consumes: `BusinessException(int code, String message)` and `Result.failure(int code, String message)`.
- Produces: integer constants used by every `new BusinessException(...)` call in `UserServiceImpl`.

- [ ] **Step 1: Run the failing compilation check**

Run:

```powershell
mvn clean compile
```

Expected: `BUILD FAILURE` with eight messages stating that `java.lang.String` cannot be converted to `int` in `UserServiceImpl`.

- [ ] **Step 2: Replace the four string constants with integer constants**

Replace:

```java
private static final String ERROR_PARAM = "PARAM_ERROR";
private static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
private static final String ERROR_USER_DUPLICATE = "USER_DUPLICATE";
private static final String ERROR_USER_CREATE_FAILED = "USER_CREATE_FAILED";
```

with:

```java
private static final int ERROR_PARAM = 40001;
private static final int ERROR_USER_NOT_FOUND = 40401;
private static final int ERROR_USER_DUPLICATE = 40901;
private static final int ERROR_USER_CREATE_FAILED = 50001;
```

- [ ] **Step 3: Verify main-source compilation**

Run:

```powershell
mvn clean compile
```

Expected: `BUILD SUCCESS` and no `String`-to-`int` errors.

### Task 2: Remove the duplicate test application class

**Files:**
- Delete: `src/test/java/com/ljl/agent/AgentBackendApplication.java`
- Preserve: `src/main/java/com/ljl/agent/AgentBackendApplication.java`
- Preserve: `src/test/java/com/ljl/agent/UserMapperTest.java`
- Test: Maven test compilation and execution

**Interfaces:**
- Consumes: the production `com.ljl.agent.AgentBackendApplication` as the Spring Boot test configuration discovered by `@SpringBootTest`.
- Produces: one application class and one mapper integration test without duplicate fully qualified class names.

- [ ] **Step 1: Run test compilation before deletion**

Run:

```powershell
mvn test -DskipTests
```

Expected: test compilation fails because `com.ljl.agent.AgentBackendApplication` exists in both main and test source sets.

- [ ] **Step 2: Delete only the duplicate test-side application class**

Delete this file:

```text
src/test/java/com/ljl/agent/AgentBackendApplication.java
```

Do not delete or edit the production application class or `UserMapperTest`.

- [ ] **Step 3: Verify test compilation**

Run:

```powershell
mvn test -DskipTests
```

Expected: `BUILD SUCCESS` with test sources compiled successfully.

- [ ] **Step 4: Run the integration test**

Supply the already provided MySQL password only in the Maven process environment, run:

```powershell
mvn test
```

Expected: `UserMapperTest` passes and Maven reports `BUILD SUCCESS` with zero failures and zero errors.

### Task 3: Align MyBatis with Spring Boot 4

**Files:**
- Modify: `pom.xml`
- Test: `src/test/java/com/ljl/agent/UserMapperTest.java`

**Interfaces:**
- Consumes: Spring Boot `4.1.0`, `@Mapper` on `UserMapper`, and the configured `DataSource`.
- Produces: MyBatis auto-configuration that creates the `UserMapper` bean.

- [ ] **Step 1: Record the failing integration test**

Run `mvn test` with the database password supplied only to the Maven process environment.

Expected: one test error caused by `NoSuchBeanDefinitionException: No qualifying bean of type 'com.ljl.agent.mapper.UserMapper'`.

- [ ] **Step 2: Upgrade the MyBatis Spring Boot Starter**

Replace:

```xml
<version>3.0.5</version>
```

with:

```xml
<version>4.0.1</version>
```

- [ ] **Step 3: Verify mapper auto-configuration and integration behavior**

Run `mvn test` with the database password supplied only to the Maven process environment.

Expected: Spring registers `UserMapper`; `UserMapperTest` passes with zero failures and zero errors.

### Task 4: Activate the integration-test database profile

**Files:**
- Modify: `src/test/java/com/ljl/agent/UserMapperTest.java`
- Preserve: `src/main/resources/application.yml`
- Consume: `src/main/resources/application-dev.yml`

**Interfaces:**
- Consumes: the `dev` profile's MySQL datasource settings.
- Produces: an integration-test ApplicationContext with a configured MySQL `DataSource`.

- [ ] **Step 1: Record the missing datasource configuration failure**

Run `mvn test` with the database password supplied only to the Maven process environment.

Expected: `Failed to determine a suitable driver class` because no profile is active.

- [ ] **Step 2: Activate the dev profile only for the mapper integration test**

Add:

```java
import org.springframework.test.context.ActiveProfiles;
```

and:

```java
@ActiveProfiles("dev")
```

to `UserMapperTest`.

- [ ] **Step 3: Verify the integration test**

Run `mvn test` with the database password supplied only to the Maven process environment.

Expected: Spring loads `application-dev.yml`, connects to MySQL, and passes `UserMapperTest`.

### Task 5: Final regression verification

**Files:**
- Inspect: `src/main/java/com/ljl/agent/service/impl/UserServiceImpl.java`
- Inspect: `src/main/java/com/ljl/agent/exception/BusinessException.java`
- Inspect: `src/main/java/com/ljl/agent/common/Result.java`
- Inspect: `src/test/java/com/ljl/agent/UserMapperTest.java`

**Interfaces:**
- Consumes: the completed changes from Tasks 1 and 2.
- Produces: evidence that the original compilation errors are gone and the test source layout is correct.

- [ ] **Step 1: Verify source layout and code types**

Confirm:

```text
src/main/java/com/ljl/agent/UserMapperTest.java does not exist
src/test/java/com/ljl/agent/UserMapperTest.java exists
src/test/java/com/ljl/agent/AgentBackendApplication.java does not exist
UserServiceImpl ERROR_* constants are int
BusinessException code is int
Result code is int
```

- [ ] **Step 2: Run the complete clean test lifecycle**

Supply the MySQL password only to the command process and run:

```powershell
mvn clean test
```

Expected: `BUILD SUCCESS`, one mapper test executed, zero failures, and zero errors.

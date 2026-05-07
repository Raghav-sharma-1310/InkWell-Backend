# SonarQube Usage in InkWell

## Overview
SonarQube is used for **static code analysis** and **test coverage reporting** across all Java microservices.

## Configuration

### Root POM (`pom.xml`)
```xml
<sonar.projectKey>inkwell-platform</sonar.projectKey>
<sonar.projectName>InkWell Platform</sonar.projectName>
<sonar.java.coveragePlugin>jacoco</sonar.java.coveragePlugin>
<sonar.coverage.jacoco.xmlReportPaths>${project.build.directory}/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
<sonar.sourceEncoding>UTF-8</sonar.sourceEncoding>
```

### Coverage Exclusions
The following are excluded from coverage calculation:
- `**/config/**` — Spring configuration classes
- `**/*Application.java` — Main application classes
- `**/dto/**` — Data transfer objects
- `**/entity/**` — JPA entities
- `**/security/OAuth2SuccessHandler.java` — OAuth2 handler

### JaCoCo Plugin
- **Version**: 0.8.12
- **Execution phases**: `prepare-agent` (before tests) + `report` (during verify)
- Applied to all child modules via `<plugins>` in root POM

### SonarQube Scanner
- **Version**: 4.0.0.4121
- **Plugin**: `sonar-maven-plugin`

## Running Analysis

```bash
# Run tests and generate coverage
mvn clean verify

# Push to SonarQube
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
```

## Frontend (Vitest)
- **Config**: `sonar-project.properties` in `frontend-web/`
- **Coverage tool**: Vitest with coverage reporter
- **Target**: 90% code coverage

## Quality Gates
- Backend: 80%+ coverage enforced via JaCoCo
- Frontend: 90% coverage target via Vitest

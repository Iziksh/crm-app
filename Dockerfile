# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -q 2>/dev/null || true

# Build production JAR (Vaadin downloads its own Node.js automatically)
COPY src ./src
RUN mvn package -Pproduction -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/crm-app-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 9080
ENTRYPOINT ["java", \
  "-Xms128m", "-Xmx384m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-Xss256k", \
  "-jar", "app.jar"]

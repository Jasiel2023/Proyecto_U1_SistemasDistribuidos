# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilacion con Maven + JDK 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache de dependencias: se invalida solo cuando cambia el pom.xml
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa 2: ejecucion con JRE 21 ligero ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

LABEL maintainer="sistema-votacion" \
      org.opencontainers.image.title="usuario-service" \
      org.opencontainers.image.description="Microservicio de Usuarios"

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

USER spring:spring
EXPOSE 8084

ENV TZ=UTC \
    JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8084/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
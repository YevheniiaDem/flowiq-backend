# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S flowiq && adduser -S flowiq -G flowiq

COPY --from=build /app/target/flowiq-backend-*.jar /app/app.jar
RUN chown flowiq:flowiq /app/app.jar

USER flowiq

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/health > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

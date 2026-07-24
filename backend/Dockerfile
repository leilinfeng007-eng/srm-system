FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package

FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --uid 10001 --create-home srm
WORKDIR /app
COPY --from=build /workspace/target/srm-backend-*.jar app.jar
USER srm
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]

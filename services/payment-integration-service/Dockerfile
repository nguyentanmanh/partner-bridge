FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B clean package -DskipTests
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

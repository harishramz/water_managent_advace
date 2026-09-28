FROM maven:3.9.9-eclipse-temurin-24 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /workspace/target/product-api-*.jar /app/product-api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/product-api.jar"]
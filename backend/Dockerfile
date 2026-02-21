FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -DskipTests clean package -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/forensic-1.0.0-SNAPSHOT.jar app.jar

# Create logs directory for Logstash
RUN mkdir -p /app/logs

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]

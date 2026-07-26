# Build stage
FROM maven:3.9.9-eclipse-temurin-24-noble AS build
WORKDIR /app

# Copy only the POM file first to cache dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Now copy the source code (which changes more frequently)
COPY src ./src/

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:24-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/mcp-server-template-0.1.0-SNAPSHOT.jar /app/application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]

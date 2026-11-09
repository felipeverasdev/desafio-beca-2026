# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven Wrapper and POM
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Normalize CRLF line endings for Linux and grant execute permission
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies in advance for caching
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]


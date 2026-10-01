# Multi-stage Dockerfile for NMAP Spring Boot Backend
# Compatible with Java JDK 25 and Spring Boot 3.5.0

# Stage 1: Build artifact with JDK 25 and Maven
FROM eclipse-temurin:25-jdk-noble AS builder

# Install Maven
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and compile executable JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime container
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

# Run as non-root user for security best practices
RUN groupadd -r nmap && useradd -r -g nmap nmap

# Copy built JAR from builder stage
COPY --from=builder /workspace/target/network-management-backend-1.0.0-SNAPSHOT.jar app.jar
RUN chown -R nmap:nmap /app

USER nmap

# Render injects PORT at runtime; default to 8080 locally
ENV PORT=8080
EXPOSE ${PORT}

# Launch Spring Boot with dynamic port binding
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]

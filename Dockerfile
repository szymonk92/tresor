# Multi-stage build for Tresor API
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM and all module POMs
COPY pom.xml .
COPY tresor-core/pom.xml ./tresor-core/
COPY tresor-api/pom.xml ./tresor-api/
COPY tresor-storage-arweave/pom.xml ./tresor-storage-arweave/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY tresor-core/src ./tresor-core/src
COPY tresor-api/src ./tresor-api/src
COPY tresor-storage-arweave/src ./tresor-storage-arweave/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="support@tresor.io"
LABEL description="Tresor API - Time-locked message delivery service"
LABEL version="1.0.0"

# Create non-root user
RUN groupadd -r tresor && useradd -r -g tresor tresor

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/tresor-api/target/*.jar app.jar

# Change ownership
RUN chown -R tresor:tresor /app

# Switch to non-root user
USER tresor

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/messages/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

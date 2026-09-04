# Stage 1: Build the JAR with dependency caching
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy only Gradle files first (caches dependencies)
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

# Make gradlew executable and download dependencies (cached)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Copy the rest of the source code
COPY src src

# Build the JAR (uses cached dependencies)
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Run the JAR
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

COPY src/main/resources/templates /app/templates/

# Port mapping
EXPOSE 8081

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
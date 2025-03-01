# Stage 1: Build Application (Optimized for Caching)
FROM gradle:latest AS build
WORKDIR /home/gradle/app

# Copy Gradle build files first (to optimize caching)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies and cache them
RUN gradle dependencies --no-daemon

# Copy the application source code AFTER dependencies are cached
COPY src ./src

# Build the fat JAR
RUN gradle buildFatJar --no-daemon --debug

# Stage 2: Create the Runtime Image
FROM amazoncorretto:22 AS runtime
EXPOSE 8080
WORKDIR /app

# Copy the built fat JAR from the build stage
COPY --from=build /home/gradle/app/build/libs/*.jar /app/ktor-docker-sample.jar

# Use a non-root user (security best practice)
RUN useradd -m ktoruser
USER ktoruser

# Run the application
ENTRYPOINT ["java", "-jar", "/app/ktor-docker-sample.jar"]
# Stage 1: Build Application with Gradle Caching
FROM gradle:latest AS build
WORKDIR /home/gradle/app

# Copy only Gradle wrapper and settings first to leverage caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies (this step will be cached unless dependencies change)
RUN gradle dependencies --no-daemon

# Copy the application source code
COPY src ./src

# Build the fat JAR (adjust task name if needed)
RUN gradle buildFatJar --no-daemon --info

# Stage 2: Create the Runtime Image
FROM amazoncorretto:22-alpine AS runtime
WORKDIR /app
EXPOSE 8080

# Copy the built fat JAR from the build stage
COPY --from=build /home/gradle/app/build/libs/*.jar /app/ktor-docker-sample.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app/ktor-docker-sample.jar"]
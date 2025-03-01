# Stage 1: Build Application (without caching)
FROM gradle:latest AS build
WORKDIR /home/gradle/app
# Copy build configuration files and the Gradle wrapper setup
COPY build.gradle.* gradle.properties ./
COPY gradle ./gradle
# Copy the application source code
COPY src/main/kotlin ./src/main/kotlin
# Build the fat JAR (adjust the task name if needed)
RUN gradle buildFatJar --no-daemon

# Stage 2: Create the Runtime Image
FROM amazoncorretto:22 AS runtime
EXPOSE 8080
RUN mkdir /app
# Copy the built fat JAR from the build stage
COPY --from=build /home/gradle/app/build/libs/*.jar /app/ktor-docker-sample.jar
ENTRYPOINT ["java", "-jar", "/app/ktor-docker-sample.jar"]
# Stage 1: Cache Gradle dependencies
FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application using ShadowJar
FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY src/main/kotlin /usr/src/app/
WORKDIR /usr/src/app
COPY --chown=gradle:gradle src/main/kotlin /home/gradle/src
WORKDIR /home/gradle/src
# Build the shadow JAR (ensure the Shadow plugin is applied in your build.gradle.kts)
RUN gradle shadowJar --no-daemon

# Stage 3: Create the Runtime Image
FROM amazoncorretto:22 AS runtime
EXPOSE 8080
RUN mkdir /app
# Adjust the jar name as needed. The default output is typically "your-app-name-all.jar"
COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/ktor-docker-sample.jar
ENTRYPOINT ["java", "-jar", "/app/ktor-docker-sample.jar"]
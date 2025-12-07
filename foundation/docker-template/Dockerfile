FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy only dependency-related files first (these change rarely)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Download dependencies (this layer will be cached unless dependencies change)
RUN gradle dependencies --no-daemon || true

# Now copy the source code (this changes frequently)
COPY src ./src

# Build the application (dependencies are already cached)
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

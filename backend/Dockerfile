# ---------- Etapa 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew gradlew
COPY build.gradle settings.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/app.jar app.jar

# Render inyecta PORT; la app ya lee ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

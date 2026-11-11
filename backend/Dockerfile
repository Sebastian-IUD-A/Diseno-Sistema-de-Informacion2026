# =====================================================================
# Imagen del backend en dos etapas:
#  1) "build": usa Maven + JDK 17 para compilar y empaquetar el .jar
#  2) imagen final: solo el JRE 17 y el .jar (más liviana y segura)
# =====================================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom.xml: Docker reutiliza esta capa (dependencias descargadas)
# mientras el pom no cambie, así las reconstrucciones son mucho más rápidas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Las pruebas se ejecutan aparte con "mvn test" (las de integración necesitan Docker).
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=America/Bogota
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/parqueadero-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

# Kapikua API: imagen para Railway (Java 21).
# 1) Compila el jar con Maven (sin pruebas: las de integración necesitan una base de pruebas).
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests -Djacoco.skip=true package

# 2) Imagen final solo con el JRE y el jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construccion /app/target/*.jar app.jar
# Railway define PORT; la app lo lee en application.properties (server.port=${PORT:8080}).
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

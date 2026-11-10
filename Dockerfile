FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests -Djacoco.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construccion /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

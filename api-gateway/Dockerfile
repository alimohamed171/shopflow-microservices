FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m -Dhttps.protocols=TLSv1.2 -Djdk.tls.client.protocols=TLSv1.2"

COPY api-gateway/pom.xml ./pom.xml

COPY api-gateway/src ./src
#RUN mvn -B dependency:resolve -DskipTests
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY order-service/pom.xml ./pom.xml
COPY order-service/src     ./src
RUN mvn -B package -DskipTests \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 \
    -Dmaven.wagon.http.retryHandler.count=3

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

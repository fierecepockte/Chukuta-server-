FROM maven:3.10.0-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S core && adduser -S -G core core
COPY --from=build /build/target/core.jar /app/core.jar

USER core
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/core.jar"]

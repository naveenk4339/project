# Builds any one service: docker build --build-arg MODULE=checkout-service -t pos/checkout-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl ${MODULE} -am package -DskipTests \
 && cp ${MODULE}/target/${MODULE}-*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
USER app
COPY --from=build /app.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

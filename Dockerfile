FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
ENV TZ=Africa/Nairobi
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Africa/Nairobi -XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
ENTRYPOINT ["java","-jar","app.jar"]
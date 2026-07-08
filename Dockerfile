FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x ./mvnw \
    && ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/target/manager-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]

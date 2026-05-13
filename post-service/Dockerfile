FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/post-service-1.0.0.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

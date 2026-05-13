FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/payment-service-1.0.0.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

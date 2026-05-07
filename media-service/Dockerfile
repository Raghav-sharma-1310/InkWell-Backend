# Runtime image for media upload and storage APIs.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/media-service-1.0.0.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

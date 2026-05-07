# Runtime image for category and tag APIs.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/category-service-1.0.0.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

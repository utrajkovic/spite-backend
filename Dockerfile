FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Instaliraj ffmpeg i maven
RUN apk add --no-cache maven ffmpeg

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

EXPOSE 8080
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
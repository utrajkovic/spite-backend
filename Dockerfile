# 🧱 Koristi Javu 17
FROM eclipse-temurin:17-jdk-alpine

# 📂 Postavi radni direktorijum
WORKDIR /app

# 🧩 Kopiraj Maven fajlove
COPY pom.xml .
COPY src ./src

# 🔨 Build projekat
RUN apk add --no-cache maven && mvn clean package -DskipTests

# 🚀 Pokreni aplikaciju
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]

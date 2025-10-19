FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app
COPY . .

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

RUN mvn clean package -DskipTests

# Stage 2: Runtime - Use JRE for smaller final image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

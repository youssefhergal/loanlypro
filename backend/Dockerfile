# Stage 1 : build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copier le pom et télécharger les dépendances
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

# Copier le code et construire le JAR
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2 : exécution
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

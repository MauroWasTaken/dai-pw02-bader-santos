FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/dai-pw02-bader-santos-1.0-SNAPSHOT.jar app.jar
EXPOSE 42069
ENTRYPOINT ["java", "-jar", "app.jar", "server"]

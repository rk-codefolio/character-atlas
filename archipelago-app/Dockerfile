FROM eclipse-temurin:25-jdk AS build
WORKDIR /source
COPY catalog-core catalog-core
COPY archipelago-app/build.gradle archipelago-app/settings.gradle archipelago-app/
COPY archipelago-app/src archipelago-app/src
RUN cd archipelago-app && ../catalog-core/gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /source/archipelago-app/build/libs/*.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

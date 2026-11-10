FROM eclipse-temurin:25-jdk AS build
WORKDIR /source
COPY catalog-core catalog-core
COPY portfolio-demo/build.gradle portfolio-demo/settings.gradle portfolio-demo/
COPY portfolio-demo/src portfolio-demo/src
RUN cd portfolio-demo && ../catalog-core/gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /source/portfolio-demo/build/libs/*.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

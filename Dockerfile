FROM openjdk:17-jdk

WORKDIR /app

COPY target/rentalplatform-app-0.0.1-SNAPSHOT.jar rentalplatform.jar

ENTRYPOINT ["java", "-jar", "rentalplatform.jar"]
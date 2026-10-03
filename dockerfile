FROM openjdk:25-rc
COPY target/Trayectico-Servidor-0.0.1-SNAPSHOT.jar ./app.jar
CMD ["java","-jar","app.jar"]

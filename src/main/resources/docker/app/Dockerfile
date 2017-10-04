FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD stockpredictionapplication.jar stockpredictionapplication.jar
ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /stockpredictionapplication.jar
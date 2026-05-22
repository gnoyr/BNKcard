# 기존의 openjdk 대신 현재 가장 권장되는 eclipse-temurin(경량화 버전)을 사용합니다.
FROM eclipse-temurin:21-jdk-alpine

VOLUME /tmp

ARG JAR_FILE=build/libs/*SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
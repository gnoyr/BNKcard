# 1. 사용할 Java 버전 (프로젝트 버전에 맞게 17 또는 21 등으로 수정하세요)
FROM openjdk:17-jdk-slim

# 2. 컨테이너 내부에 임시 파일이 생성될 볼륨 설정
VOLUME /tmp

# 3. GitHub Actions가 빌드한 jar 파일의 위치를 변수로 설정
ARG JAR_FILE=build/libs/*SNAPSHOT.jar

# 4. jar 파일을 컨테이너 내부의 app.jar라는 이름으로 복사
COPY ${JAR_FILE} app.jar

# 5. 컨테이너가 켜질 때 실행할 명령어 (스프링 부트 실행)
ENTRYPOINT ["java", "-jar", "/app.jar"]
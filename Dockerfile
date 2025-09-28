# 실행에 필요한 Java 17 JRE 이미지를 기반으로 시작
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# GitHub Actions 워크플로우에서 미리 빌드한 .jar 파일을 복사
COPY target/*.jar app.jar

# 8080 포트 노출
EXPOSE 8080

# 애플리케이션 실행 명령어
ENTRYPOINT ["java","-jar","app.jar"]
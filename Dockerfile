# 1. 빌드 환경
# Maven 과 Java 17 이 설치된 이미지를 기반으로 'build' 라는 이름의 스테이지를 시작한다.
FROM maven:3.9.6-eclipse-temurin-17 AS build

# 작업 디렉토리를 /app 으로 설정한다.
WORKDIR /app

# 현재 폴더의 모든 파일(소스코드, pom.xml 등) 을 컨테이너의 /app 폴더로 복사한다.
COPY . .

# Maven Wrapper 파일에 실행 권한 부여
RUN chmod +x mvnw

# Maven Wrapper 를 사용하여 프로젝트를 빌드한다.
RUN ./mvnw clean package -DskipTests

# 2단계 실행(Run Time) 환경
# 더 가벼운 Java 17 실행 환경(JRE) 이미지를 기반으로 최종 이미지를 만든다.
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리를 /app 으로 설정한다.
WORKDIR /app

# 1 단계(build) 스테이지에서 생성된 .jar 파일을 컨테이너 /app 폴더로 "app.jar" 라는 이름으로 복사한다.
COPY --from=build /app/target/*.jar app.jar

# 컨테이너의 8080 포트를 외부에 노출한다.
EXPOSE 8080

# 컨테이너가 시작될 때 실행될 기본 명령얼르 설정한다.
ENTRYPOINT [ "java" ,"-jar", "app.jar" ]


#   * 1단계 (빌드 환경):
#       * 먼저, Maven과 Java 17이 모두 설치된 '빌드용' 컨테이너 환경을 만듭니다.
#       * 이곳에 우리 프로젝트 코드를 복사한 후, mvnw clean package 명령으로 .jar 파일을 빌드합니다.
#
#
#   * 2단계 (실행 환경):
#       * 이제 완전히 새로운, 더 가볍고 깨끗한 '실행용' 컨테이너 환경을 만듭니다. 여기에는 무거운 Maven
#         없이, 딱 Java 17 실행 환경(JRE)만 들어있습니다.
#       * 1단계에서 만들었던 .jar 파일만 이곳으로 복사해옵니다.
#       * EXPOSE 8080으로 8080 포트를 열어주고, ENTRYPOINT로 애플리케이션을 실행할 명령어를 지정합니다.
#
#
#  이렇게 하면, 최종 이미지에는 무거운 빌드 도구나 소스 코드가 전혀 남지 않고, 딱 실행에 필요한 .jar
#  파일과 Java 실행 환경만 남게 되어 훨씬 가볍고 안전한 이미지가 만들어집니다.

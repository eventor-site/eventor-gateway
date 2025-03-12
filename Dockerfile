# OpenJDK 21 기반 이미지 사용
FROM eclipse-temurin:21-jdk

ARG EUREKA_USERNAME=${EUREKA_USERNAME}
ARG EUREKA_PASSWORD=${EUREKA_PASSWORD}


ENV EUREKA_USERNAME=${EUREKA_USERNAME}
ENV EUREKA_PASSWORD=${EUREKA_PASSWORD}

WORKDIR /app

# JAR 파일 복사
COPY target/eventor-gateway-0.0.1-SNAPSHOT.jar eventor-gateway.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "eventor-gateway.jar"]
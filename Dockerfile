# syntax=docker/dockerfile:1.7

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml .
COPY vsdu-boot/pom.xml vsdu-boot/pom.xml
COPY vsdu-business/pom.xml vsdu-business/pom.xml
COPY vsdu-common/pom.xml vsdu-common/pom.xml
COPY vsdu-infrastructure/pom.xml vsdu-infrastructure/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package


# ---------- 运行阶段 ----------
FROM eclipse-temurin:25-jre

WORKDIR /vsdu

COPY --from=builder /app/vsdu-boot/target/*.jar vsdu-app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "vsdu-app.jar"]

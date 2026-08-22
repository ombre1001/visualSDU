# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests


# ---------- 运行阶段 ----------
FROM eclipse-temurin:25-jre

WORKDIR /vsdu

COPY --from=builder /app/vsdu-boot/target/*.jar vsdu-app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "vsdu-app.jar"]
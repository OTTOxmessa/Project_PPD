# syntax=docker/dockerfile:1
# Build context คือโฟลเดอร์ code/  (docker build -t portfolio-system code)

# ---------- Stage 1: build React ----------
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---------- Stage 2: build Spring Boot jar (รวมไฟล์ React ไว้ใน static/) ----------
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY src/ src/
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN sh mvnw -B -q package -DskipTests

# ---------- Stage 3: run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=backend /app/target/*.jar app.jar
USER app
# ค่าฐานข้อมูลและ JWT_SECRET ส่งเป็น environment variable ตอนรัน ห้ามใส่ในไฟล์นี้
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

# ===== 后端构建 =====
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /build
COPY payment-system/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY payment-system/src ./src
COPY payment-system/sql ./sql
RUN mvn package -DskipTests -B

# ===== 后端运行 =====
FROM eclipse-temurin:17-jre-alpine AS backend
WORKDIR /app
COPY --from=backend-build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# ===== 前端构建 =====
FROM node:18-alpine AS frontend-build
WORKDIR /build
COPY salessystem/package.json salessystem/package-lock.json* ./
RUN npm ci --prefer-offline
COPY salessystem/ .
RUN npm run build

# ===== 前端运行（Nginx）=====
FROM nginx:alpine AS frontend
COPY --from=frontend-build /build/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

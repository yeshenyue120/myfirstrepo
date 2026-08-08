# ===== 构建阶段：编译并打包 jar =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先拷贝 pom.xml 单独拉依赖，利用 Docker 层缓存加速重复构建
COPY pom.xml .
RUN mvn -q dependency:go-offline

# 拷贝源码并打包（跳过测试，单测在 CI 里跑）
COPY src ./src
RUN mvn -q clean package -DskipTests

# ===== 运行阶段：最小 JRE 镜像只跑 jar =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 非 root 用户运行，提升容器安全性
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# 多阶段构建: 第一阶段 Maven 编译打包, 第二阶段仅含 JRE 运行时
# 供 Render / Railway / 本地 docker build 使用
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/donkey-trace-backend-1.0.0.jar app.jar
# 数据库连接通过环境变量注入: DB_URL / DB_USER / DB_PASS
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

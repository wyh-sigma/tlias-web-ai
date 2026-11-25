# 构建阶段：用 Maven 镜像编译，产物与构建环境解耦
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# 运行阶段：只保留 JRE 和 jar，镜像更小
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/data /app/logs \
    && chown -R app:app /app
COPY --from=build /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

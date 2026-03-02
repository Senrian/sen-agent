# sen-agent Dockerfile - 部署友好
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 安装Maven
RUN apk add --no-cache maven

# 复制源码
COPY pom.xml .
COPY src ./src

# 构建
RUN mvn clean package -DskipTests

# 运行镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非root用户
RUN addgroup -S senagent && adduser -S senagent -G senagent

# 复制jar
COPY --from=builder /app/target/sen-agent-*.jar app.jar

# 创建目录
RUN mkdir -p /tmp/sen-agent-files /logs && chown -R senagent:senagent /tmp/sen-agent-files /logs

# 环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SERVER_PORT=8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# 切换用户
USER senagent

# 暴露端口
EXPOSE 8080

# 启动
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ============================================================
# Graphiti-Java Docker 打包镜像
# 注意：需要先本地构建前后端，此镜像仅用于打包和运行
# ============================================================
FROM docker.1ms.run/library/eclipse-temurin:21-jre-alpine AS runtime

# 安装运行时依赖
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories \
    && apk add --no-cache curl bash tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户
RUN addgroup -g 1001 -S graphiti && adduser -u 1001 -S graphiti -G graphiti

WORKDIR /app

# 从本地构建产物复制 JAR 文件
COPY ontograph-backend/target/*.jar app.jar

# 配置目录
RUN mkdir -p /app/config /app/logs && chown -R graphiti:graphiti /app

USER graphiti

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness \
    || exit 1

# 暴露端口
EXPOSE 8080

# JVM 调优参数
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

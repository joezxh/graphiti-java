# ============================================================
# Graphiti-Java 多阶段 Docker 构建
# ============================================================
# Stage 1: Build - 编译前端 + 后端
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 预装 Node.js + pnpm (用于构建前端)
RUN apt-get update && apt-get install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g pnpm@9 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 拷贝前端源码并构建
COPY graphiti-web/package*.json graphiti-web/pnpm-lock.yaml graphiti-web/vite.config.ts ./
COPY graphiti-web/src ./graphiti-web/src
WORKDIR /build/graphiti-web
RUN pnpm install --frozen-lockfile && pnpm build

# 拷贝前端构建产物到后端静态资源目录
RUN mkdir -p /build/graphiti-server/src/main/resources && \
    cp -r /build/graphiti-web/dist /build/graphiti-server/src/main/resources/static

# 拷贝后端源码
WORKDIR /build
COPY graphiti-server/pom.xml graphiti-server/
COPY graphiti-framework graphiti-framework
COPY graphiti-module-system graphiti-module-system
COPY graphiti-module-core graphiti-module-core

# Maven 构建 (跳过测试)
WORKDIR /build/graphiti-server
RUN mvn clean package -DskipTests

# ============================================================
# Stage 2: Runtime - 运行镜像
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# 安装运行时依赖 (ca-certificates 用于 HTTPS 调用 LLM API)
RUN apk add --no-cache curl bash tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户
RUN addgroup -g 1001 -S graphiti && adduser -u 1001 -S graphiti -G graphiti

WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/graphiti-server/target/graphiti-server-*.jar app.jar

# 配置目录
RUN mkdir -p /app/config /app/logs && chown -R graphiti:graphiti /app

USER graphiti

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness \
    || exit 1

# 暴露端口
EXPOSE 8080

# JVM 调优参数 (可通过环境变量覆盖)
# -XX:+UseContainerSupport: 让 JVM 自动感知容器内存限制
# -XX:MaxRAMPercentage: 建议设置为 70-75%，留内存给操作系统
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

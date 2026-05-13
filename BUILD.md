# Graphiti-Java 本地构建说明

## 快速开始

### 方式一：使用构建脚本（推荐）

```bash
# Linux/macOS
chmod +x scripts/build.sh
./scripts/build.sh

# Windows PowerShell
.\\scripts\\build.ps1
```

### 方式二：手动构建

```bash
# 1. 构建前端
cd graphiti-web
pnpm install
pnpm build

# 2. 拷贝前端产物到后端
mkdir -p ../graphiti-server/src/main/resources/static
cp -r dist/* ../graphiti-server/src/main/resources/static/

# 3. 构建后端
cd ..
mvn clean package -DskipTests

# 4. 启动 Docker
docker-compose up -d
```

## Docker 启动

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f graphiti-java

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build
```

## 访问服务

- **应用地址**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API 文档**: http://localhost:8080/v3/api-docs

## 环境变量配置

复制 `.env.example` 为 `.env` 并修改配置：

```bash
cp .env.example .env
# 编辑 .env 文件
```

关键配置项：
- `SPRING_AI_OPENAI_API_KEY` - OpenAI API 密钥
- `NEO4J_PASSWORD` - Neo4j 数据库密码
- `POSTGRES_PASSWORD` - PostgreSQL 数据库密码
- `JWT_SECRET` - JWT 签名密钥

## 常见问题

### 1. 前端构建失败

```bash
# 清理缓存并重新安装
cd graphiti-web
rm -rf node_modules .pnpm-store
pnpm install
pnpm build
```

### 2. Maven 构建失败

```bash
# 清理本地缓存
mvn clean
rm -rf ~/.m2/repository/com/graphiti

# 重新构建
mvn clean package -DskipTests
```

### 3. Docker 启动失败

```bash
# 查看日志
docker-compose logs graphiti-java

# 检查端口占用
netstat -tuln | grep 8080

# 重新构建镜像
docker-compose build --no-cache
docker-compose up -d
```

## 生产环境部署

```bash
# 使用生产配置
docker-compose -f docker-compose.prod.yml up -d

# 或使用环境变量
SPRING_PROFILES_ACTIVE=prod docker-compose up -d
```

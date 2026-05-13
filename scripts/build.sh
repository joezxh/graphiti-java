#!/bin/bash
# ============================================================
# Graphiti-Java 本地构建脚本
# 用于在 Docker 打包前构建前后端
# ============================================================

set -e  # 遇到错误立即退出

echo "========================================="
echo "  Graphiti-Java 本地构建"
echo "========================================="

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo ""
echo "[1/3] 构建前端..."
echo "-----------------------------------------"
cd graphiti-web

if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    pnpm install
fi

echo "构建前端..."
pnpm build

# 创建后端静态资源目录
mkdir -p ../graphiti-server/src/main/resources/static

# 拷贝前端构建产物
echo "拷贝前端产物到后端..."
cp -r dist/* ../graphiti-server/src/main/resources/static/

cd "$PROJECT_ROOT"

echo ""
echo "[2/3] 构建后端..."
echo "-----------------------------------------"
echo "Maven 构建 (跳过测试)..."
mvn clean package -Dmaven.test.skip=true

echo ""
echo "[3/3] 验证构建产物..."
echo "-----------------------------------------"
if ls graphiti-server/target/*.jar 1> /dev/null 2>&1; then
    JAR_FILE=$(ls graphiti-server/target/*.jar | head -1)
    echo "✓ 构建成功: $JAR_FILE"
    echo "  文件大小: $(du -h "$JAR_FILE" | cut -f1)"
else
    echo "✗ 构建失败: 未找到 JAR 文件"
    exit 1
fi

echo ""
echo "========================================="
echo "  构建完成！"
echo "========================================="
echo ""
echo "下一步："
echo "  启动 Docker: docker-compose up -d"
echo "  查看日志:    docker-compose logs -f graphiti-java"
echo ""

#!/bin/bash
# ============================================================
# Graphiti-Java 构建脚本
# ============================================================
# 用法:
#   ./scripts/build.sh              # 构建 Docker 镜像 (默认 latest)
#   ./scripts/build.sh v1.0.0       # 构建指定版本镜像
#   ./scripts/build.sh --no-cache    # 不使用缓存构建
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 默认变量
VERSION="${1:-latest}"
NO_CACHE=""
IMAGE_NAME="graphiti-java"
REGISTRY=""

# 解析参数
if [[ "$1" == "--no-cache" ]]; then
    NO_CACHE="--no-cache"
    VERSION="${2:-latest}"
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Graphiti-Java Docker 构建脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo "版本: $VERSION"
echo "项目根目录: $PROJECT_ROOT"
echo ""

# 检查环境
echo -e "${YELLOW}[1/4] 检查环境...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装或不在 PATH 中${NC}"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker daemon 未运行${NC}"
    exit 1
fi

if [ ! -f "$PROJECT_ROOT/docker/Dockerfile" ]; then
    echo -e "${RED}错误: Dockerfile 不存在于 $PROJECT_ROOT/docker/${NC}"
    exit 1
fi

echo -e "${GREEN}  Docker: OK${NC}"
echo -e "${GREEN}  Dockerfile: OK${NC}"

# 前端依赖检查
echo -e "${YELLOW}[2/4] 检查前端依赖...${NC}"
if [ -f "$PROJECT_ROOT/graphiti-web/package.json" ]; then
    if [ ! -f "$PROJECT_ROOT/graphiti-web/pnpm-lock.yaml" ]; then
        echo -e "${YELLOW}  警告: 未找到 pnpm-lock.yaml，可能需要先安装依赖${NC}"
    else
        echo -e "${GREEN}  前端: OK${NC}"
    fi
else
    echo -e "${YELLOW}  警告: 未找到 graphiti-web/package.json，跳过前端构建检查${NC}"
fi

# 清理旧构建产物
echo -e "${YELLOW}[3/4] 清理旧构建产物...${NC}"
rm -rf "$PROJECT_ROOT/graphiti-server/src/main/resources/static"
echo -e "${GREEN}  已清理旧构建产物${NC}"

# Docker 构建
echo -e "${YELLOW}[4/4] 构建 Docker 镜像...${NC}"
FULL_IMAGE_NAME="${REGISTRY}${IMAGE_NAME}:${VERSION}"

echo "镜像名称: $FULL_IMAGE_NAME"
echo ""

if docker build \
    $NO_CACHE \
    -f "$PROJECT_ROOT/docker/Dockerfile" \
    -t "$FULL_IMAGE_NAME" \
    "$PROJECT_ROOT"; then

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  构建成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "  镜像: $FULL_IMAGE_NAME"
    echo ""
    echo "  下一步:"
    echo "    1. 启动服务:"
    echo "       cp .env.example .env"
    echo "       # 编辑 .env 填入实际配置"
    echo "       docker-compose --profile prod up -d"
    echo ""
    echo "    2. 查看日志:"
    echo "       docker-compose logs -f graphiti-java"
    echo ""
    echo "    3. 访问服务:"
    echo "       http://localhost:8080"
    echo ""

else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  构建失败！${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi

#!/bin/bash
# ============================================================
# Graphiti-Java 数据恢复脚本
# ============================================================
# 从备份恢复 PostgreSQL + Redis 数据
#
# 用法:
#   ./scripts/restore.sh /path/to/backup.tar.gz
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 备份文件路径
BACKUP_FILE="${1:-}"

# 检查参数
if [ -z "$BACKUP_FILE" ]; then
    echo -e "${RED}错误: 请提供备份文件路径${NC}"
    echo ""
    echo "用法:"
    echo "  $0 /path/to/backup.tar.gz"
    echo ""
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}错误: 备份文件不存在: $BACKUP_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Graphiti-Java 数据恢复脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo "备份文件: $BACKUP_FILE"
echo "项目目录: $PROJECT_ROOT"
echo ""

# 警告
echo -e "${RED}========================================${NC}"
echo -e "${RED}  ⚠️  警告 ⚠️${NC}"
echo -e "${RED}========================================${NC}"
echo "此操作将覆盖当前数据！"
echo ""
read -p "确认继续? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "已取消恢复操作"
    exit 0
fi

# 创建临时目录
TEMP_DIR="$PROJECT_ROOT/.restore_temp_$$"
mkdir -p "$TEMP_DIR"

echo -e "${YELLOW}[1/4] 解压备份文件...${NC}"
tar -xzf "$BACKUP_FILE" -C "$TEMP_DIR"

# 查找解压后的备份目录
BACKUP_CONTENT=$(ls "$TEMP_DIR" | head -1)
BACKUP_PATH="$TEMP_DIR/$BACKUP_CONTENT"

if [ ! -d "$BACKUP_PATH" ]; then
    echo -e "${RED}错误: 备份文件格式不正确${NC}"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo -e "${GREEN}  解压成功${NC}"
echo "  内容: $BACKUP_CONTENT"
echo ""

# 停止容器
echo -e "${YELLOW}[2/4] 停止相关容器...${NC}"
docker-compose -f "$PROJECT_ROOT/docker-compose.yml" stop graphiti-java postgres redis 2>/dev/null || true
docker stop graphiti-postgres graphiti-redis 2>/dev/null || true
echo -e "${GREEN}  容器已停止${NC}"
echo ""

# 恢复 PostgreSQL 数据
echo -e "${YELLOW}[3/4] 恢复 PostgreSQL 数据...${NC}"
if [ -d "$BACKUP_PATH/postgres" ]; then
    POSTGRES_DATA_DIR="$PROJECT_ROOT/data/postgres"

    # 备份当前数据 (可选)
    if [ -d "$POSTGRES_DATA_DIR" ] && [ "$(ls -A $POSTGRES_DATA_DIR 2>/dev/null)" ]; then
        echo "  备份当前 PostgreSQL 数据..."
        mv "$POSTGRES_DATA_DIR" "$POSTGRES_DATA_DIR.bak.$(date +%Y%m%d_%H%M%S)"
    fi

    # 创建数据目录
    mkdir -p "$POSTGRES_DATA_DIR"

    # 恢复数据
    cp -a "$BACKUP_PATH/postgres/." "$POSTGRES_DATA_DIR/"
    chown -R 1000:1000 "$POSTGRES_DATA_DIR" 2>/dev/null || true

    echo -e "${GREEN}  PostgreSQL 数据已恢复${NC}"
else
    echo -e "${YELLOW}  警告: 备份中未包含 PostgreSQL 数据，跳过${NC}"
fi
echo ""

# 恢复 Redis 数据
echo -e "${YELLOW}[4/4] 恢复 Redis 数据...${NC}"
if [ -d "$BACKUP_PATH/redis" ]; then
    REDIS_DATA_DIR="$PROJECT_ROOT/data/redis"

    # 备份当前数据 (可选)
    if [ -d "$REDIS_DATA_DIR" ] && [ "$(ls -A $REDIS_DATA_DIR 2>/dev/null)" ]; then
        echo "  备份当前 Redis 数据..."
        mv "$REDIS_DATA_DIR" "$REDIS_DATA_DIR.bak.$(date +%Y%m%d_%H%M%S)"
    fi

    # 创建数据目录
    mkdir -p "$REDIS_DATA_DIR"

    # 恢复数据
    cp -a "$BACKUP_PATH/redis/." "$REDIS_DATA_DIR/"
    chown -R 1000:1000 "$REDIS_DATA_DIR" 2>/dev/null || true

    echo -e "${GREEN}  Redis 数据已恢复${NC}"
else
    echo -e "${YELLOW}  警告: 备份中未包含 Redis 数据，跳过${NC}"
fi
echo ""

# 恢复配置文件
if [ -d "$BACKUP_PATH/config" ]; then
    echo -e "${YELLOW}恢复配置文件...${NC}"
    if [ -f "$BACKUP_PATH/config/.env" ]; then
        echo "  发现 .env 配置文件"
        read -p "是否覆盖当前 .env 文件? (yes/no): " RESTORE_ENV
        if [ "$RESTORE_ENV" == "yes" ]; then
            cp "$BACKUP_PATH/config/.env" "$PROJECT_ROOT/.env"
            echo -e "${GREEN}  .env 已恢复${NC}"
        else
            echo "  跳过 .env 恢复"
        fi
    fi
fi

# 清理临时目录
rm -rf "$TEMP_DIR"

# 启动容器
echo -e "${YELLOW}启动服务...${NC}"
docker-compose -f "$PROJECT_ROOT/docker-compose.yml" up -d postgres redis
sleep 5
docker-compose -f "$PROJECT_ROOT/docker-compose.yml" up -d graphiti-java

# 输出结果
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  恢复完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "下一步:"
echo "  1. 检查服务状态: docker-compose ps"
echo "  2. 查看日志: docker-compose logs -f graphiti-java"
echo "  3. 验证数据: curl http://localhost:8080/actuator/health"
echo ""

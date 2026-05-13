#!/bin/bash
# ============================================================
# Graphiti-Java 数据备份脚本
# ============================================================
# 备份 PostgreSQL + Redis 数据到 tar.gz 文件
#
# 用法:
#   ./scripts/backup.sh                    # 备份到默认位置 (./backups/)
#   ./scripts/backup.sh /path/to/backup    # 备份到指定位置
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

# 备份目录 (默认 ./backups/)
BACKUP_DIR="${1:-$PROJECT_ROOT/backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="graphiti_backup_${TIMESTAMP}"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_NAME}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Graphiti-Java 数据备份脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo "备份目录: $BACKUP_PATH"
echo ""

# 创建备份目录
echo -e "${YELLOW}[1/5] 创建备份目录...${NC}"
mkdir -p "$BACKUP_PATH"
echo -e "${GREEN}  目录已创建: $BACKUP_PATH${NC}"

# 备份 PostgreSQL 数据
echo -e "${YELLOW}[2/5] 备份 PostgreSQL 数据...${NC}"
POSTGRES_DATA_DIR="$PROJECT_ROOT/data/postgres"
if [ -d "$POSTGRES_DATA_DIR" ]; then
    # 停止 PostgreSQL 容器以确保数据一致性
    echo "  停止 PostgreSQL 容器..."
    docker stop graphiti-postgres 2>/dev/null || true

    # 等待一小段时间确保写入完成
    sleep 2

    # 复制数据目录
    mkdir -p "$BACKUP_PATH/postgres"
    cp -a "$POSTGRES_DATA_DIR/." "$BACKUP_PATH/postgres/"

    # 恢复 PostgreSQL 容器
    echo "  恢复 PostgreSQL 容器..."
    docker start graphiti-postgres 2>/dev/null || true

    echo -e "${GREEN}  PostgreSQL 数据已备份${NC}"
else
    echo -e "${YELLOW}  警告: 未找到 PostgreSQL 数据目录 ($POSTGRES_DATA_DIR)，跳过${NC}"
fi

# 备份 Redis 数据
echo -e "${YELLOW}[3/5] 备份 Redis 数据...${NC}"
REDIS_DATA_DIR="$PROJECT_ROOT/data/redis"
if [ -d "$REDIS_DATA_DIR" ]; then
    # 停止 Redis 容器
    echo "  停止 Redis 容器..."
    docker stop graphiti-redis 2>/dev/null || true

    # 等待确保写入完成
    sleep 1

    # 复制数据目录
    mkdir -p "$BACKUP_PATH/redis"
    cp -a "$REDIS_DATA_DIR/." "$BACKUP_PATH/redis/"

    # 恢复 Redis 容器
    echo "  恢复 Redis 容器..."
    docker start graphiti-redis 2>/dev/null || true

    echo -e "${GREEN}  Redis 数据已备份${NC}"
else
    echo -e "${YELLOW}  警告: 未找到 Redis 数据目录 ($REDIS_DATA_DIR)，跳过${NC}"
fi

# 备份环境配置文件
echo -e "${YELLOW}[4/5] 备份配置文件...${NC}"
mkdir -p "$BACKUP_PATH/config"
if [ -f "$PROJECT_ROOT/.env" ]; then
    cp "$PROJECT_ROOT/.env" "$BACKUP_PATH/config/"
    echo -e "${GREEN}  .env 已备份 (注意: 包含敏感信息！)${NC}"
fi
if [ -f "$PROJECT_ROOT/docker-compose.prod.yml" ]; then
    cp "$PROJECT_ROOT/docker-compose.prod.yml" "$BACKUP_PATH/config/"
fi
if [ -d "$PROJECT_ROOT/config" ]; then
    cp -r "$PROJECT_ROOT/config" "$BACKUP_PATH/config_local"
fi

# 创建备份信息文件
echo -e "${YELLOW}[5/5] 创建备份元信息...${NC}"
cat > "$BACKUP_PATH/backup_info.txt" << EOF
Graphiti-Java Backup Information
================================
Backup Date: $(date)
Backup Host: $(hostname)
Backup Path: $BACKUP_PATH
Project Root: $PROJECT_ROOT

Included:
$(ls -la "$BACKUP_PATH" 2>/dev/null || echo "  (empty)")

Docker Version:
$(docker --version 2>/dev/null || echo "Docker not available")

Container Status:
$(docker ps --filter "name=graphiti" --format "{{.Names}}: {{.Status}}" 2>/dev/null || echo "Docker not available")
EOF

# 压缩备份文件
echo ""
echo -e "${YELLOW}压缩备份文件...${NC}"
cd "$BACKUP_DIR"
tar -czf "${BACKUP_NAME}.tar.gz" -C "$BACKUP_DIR" "$BACKUP_NAME"
rm -rf "$BACKUP_PATH"

# 输出备份结果
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  备份完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}备份文件: ${NC}$BACKUP_DIR/${BACKUP_NAME}.tar.gz"
echo -e "${BLUE}备份大小: ${NC}$(du -h "${BACKUP_NAME}.tar.gz" | cut -f1)"
echo ""
echo -e "${YELLOW}重要提示:${NC}"
echo "  1. 备份文件包含 .env 中的敏感信息，请妥善保管"
echo "  2. 恢复数据: ./scripts/restore.sh $BACKUP_DIR/${BACKUP_NAME}.tar.gz"
echo ""

# 清理超过 30 天的旧备份
echo -e "${YELLOW}清理超过 30 天的旧备份...${NC}"
find "$BACKUP_DIR" -name "graphiti_backup_*.tar.gz" -mtime +30 -delete 2>/dev/null || true
echo -e "${GREEN}  清理完成${NC}"

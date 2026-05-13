#!/bin/bash
# ============================================================
# Graphiti-Java 数据备份脚本
# ============================================================
# 用法:
#   ./scripts/backup.sh                  # 备份到默认位置
#   ./scripts/backup.sh /path/to/backup  # 备份到指定目录
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 默认变量
BACKUP_DIR="${1:-$PROJECT_ROOT/backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="graphiti-backup-${TIMESTAMP}"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_NAME}"
LOG_FILE="${BACKUP_DIR}/backup.log"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Graphiti-Java 数据备份${NC}"
echo -e "${GREEN}========================================${NC}"
echo "备份目录: $BACKUP_PATH"
echo ""

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 记录日志
log() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo -e "$msg"
    echo "$msg" >> "$LOG_FILE"
}

log "========== 开始备份 =========="

# 备份 PostgreSQL
echo -e "${YELLOW}[1/3] 备份 PostgreSQL...${NC}"
log "开始备份 PostgreSQL"
if [ -d "$PROJECT_ROOT/data/postgres" ]; then
    POSTGRES_BACKUP="${BACKUP_PATH}/postgres"
    mkdir -p "$POSTGRES_BACKUP"

    # 使用 docker exec 执行 pg_dumpall
    if docker exec graphiti-postgres pg_dumpall -U postgres --clean > "${POSTGRES_BACKUP}/graphiti.sql" 2>/dev/null; then
        log "PostgreSQL 备份成功: ${POSTGRES_BACKUP}/graphiti.sql ($(du -h "${POSTGRES_BACKUP}/graphiti.sql" | cut -f1))"
        echo -e "${GREEN}  PostgreSQL: OK ($(du -h "${POSTGRES_BACKUP}/graphiti.sql" | cut -f1))${NC}"
    else
        # 如果容器未运行，尝试从数据目录备份
        log "警告: 无法通过 docker exec 备份，尝试从数据目录备份"
        if [ -f "$PROJECT_ROOT/data/postgres/postgresql.conf" ]; then
            cp -r "$PROJECT_ROOT/data/postgres" "$POSTGRES_BACKUP"
            log "PostgreSQL 数据目录备份成功: ${POSTGRES_BACKUP}"
            echo -e "${GREEN}  PostgreSQL (目录): OK${NC}"
        else
            log "警告: PostgreSQL 数据不存在，跳过"
            echo -e "${YELLOW}  PostgreSQL: 跳过 (无数据)${NC}"
        fi
    fi
else
    log "警告: PostgreSQL 数据目录不存在，跳过"
    echo -e "${YELLOW}  PostgreSQL: 跳过 (目录不存在)${NC}"
fi

# 备份 Redis
echo -e "${YELLOW}[2/3] 备份 Redis...${NC}"
log "开始备份 Redis"
if [ -d "$PROJECT_ROOT/data/redis" ]; then
    REDIS_BACKUP="${BACKUP_PATH}/redis"
    mkdir -p "$REDIS_BACKUP"

    # 使用 docker exec 执行 redis-cli BGSAVE 后复制数据
    if docker exec graphiti-redis redis-cli BGSAVE 2>/dev/null; then
        sleep 2  # 等待后台保存完成
        docker cp graphiti-redis:/data "${REDIS_BACKUP}/" 2>/dev/null || true
    fi

    # 备份数据目录
    cp -r "$PROJECT_ROOT/data/redis" "${BACKUP_PATH}/redis"
    log "Redis 备份成功: ${BACKUP_PATH}/redis ($(du -sh "${BACKUP_PATH}/redis" | cut -f1))"
    echo -e "${GREEN}  Redis: OK ($(du -sh "${BACKUP_PATH}/redis" | cut -f1))${NC}"
else
    log "警告: Redis 数据目录不存在，跳过"
    echo -e "${YELLOW}  Redis: 跳过 (目录不存在)${NC}"
fi

# 备份配置文件
echo -e "${YELLOW}[3/3] 备份配置文件...${NC}"
log "开始备份配置文件"
CONFIG_BACKUP="${BACKUP_PATH}/config"
mkdir -p "$CONFIG_BACKUP"

if [ -f "$PROJECT_ROOT/.env" ]; then
    cp "$PROJECT_ROOT/.env" "${CONFIG_BACKUP}/.env"
    log "配置文件备份成功: ${CONFIG_BACKUP}/.env"
    echo -e "${GREEN}  配置文件: OK${NC}"
else
    if [ -f "$PROJECT_ROOT/.env.example" ]; then
        cp "$PROJECT_ROOT/.env.example" "${CONFIG_BACKUP}/.env.example"
        log "配置模板备份成功: ${CONFIG_BACKUP}/.env.example"
        echo -e "${YELLOW}  配置文件: 仅模板${NC}"
    fi
fi

# 打包备份
echo ""
echo -e "${YELLOW}打包备份...${NC}"
cd "$BACKUP_DIR"
tar -czf "${BACKUP_NAME}.tar.gz" "$BACKUP_NAME"
rm -rf "$BACKUP_PATH"  # 删除未打包目录

BACKUP_SIZE=$(du -h "${BACKUP_NAME}.tar.gz" | cut -f1)
log "备份打包完成: ${BACKUP_PATH}.tar.gz (${BACKUP_SIZE})"
echo -e "${GREEN}  打包: OK (${BACKUP_SIZE})${NC}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  备份完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "备份文件: ${BACKUP_PATH}.tar.gz"
echo ""
echo "恢复命令:"
echo "  ./scripts/restore.sh ${BACKUP_PATH}.tar.gz"
echo ""
log "========== 备份完成 =========="

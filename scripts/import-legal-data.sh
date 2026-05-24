#!/bin/bash

# =====================================================
# 法律知识图谱批量数据导入脚本
# =====================================================
# 用途: 从JSON文件批量导入法律知识图谱数据
# 用法: ./scripts/import-legal-data.sh <json-file>
# =====================================================

set -e

# 配置
BASE_URL="http://localhost:8080/api/v1"
TOKEN="${GRAPHITI_TOKEN:-your-token-here}"

# 检查参数
if [ $# -eq 0 ]; then
    echo "用法: $0 <json-file>"
    echo "示例: $0 data/legal-kg-sample-data.json"
    exit 1
fi

JSON_FILE="$1"

# 检查文件是否存在
if [ ! -f "$JSON_FILE" ]; then
    echo "错误: 文件不存在 - $JSON_FILE"
    exit 1
fi

echo "========================================"
echo "法律知识图谱批量数据导入"
echo "========================================"
echo ""
echo "文件: $JSON_FILE"
echo "API: $BASE_URL"
echo ""

# 执行批量导入
echo "正在导入数据..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/graph/import" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "@${JSON_FILE}")

# 检查响应
if echo "$RESPONSE" | grep -q '"code":0'; then
    echo ""
    echo "========================================"
    echo "✓ 数据导入成功!"
    echo "========================================"
    echo ""
    echo "导入详情:"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
    echo ""
    echo "========================================"
    echo "✗ 数据导入失败"
    echo "========================================"
    echo ""
    echo "错误信息:"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    exit 1
fi

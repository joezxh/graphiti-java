# =====================================================
# 法律知识图谱批量数据导入脚本 (PowerShell版本)
# =====================================================
# 用途: 从JSON文件批量导入法律知识图谱数据
# 用法: .\scripts\import-legal-data.ps1 <json-file>
# =====================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$JsonFile
)

# 配置
$BASE_URL = "http://localhost:8080/api/v1"
$TOKEN = if ($env:GRAPHITI_TOKEN) { $env:GRAPHITI_TOKEN } else { "your-token-here" }

Write-Host "========================================" -ForegroundColor Green
Write-Host "法律知识图谱批量数据导入" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "文件: $JsonFile"
Write-Host "API: $BASE_URL"
Write-Host ""

# 检查文件是否存在
if (-not (Test-Path $JsonFile)) {
    Write-Host "错误: 文件不存在 - $JsonFile" -ForegroundColor Red
    exit 1
}

# 读取JSON文件
Write-Host "正在读取JSON文件..." -ForegroundColor Yellow
$jsonContent = Get-Content $JsonFile -Raw | ConvertFrom-Json

# 执行批量导入
Write-Host "正在导入数据..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/graph/import" `
        -Method Post `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body (ConvertTo-Json $jsonContent -Depth 10)

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ 数据导入成功!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "导入详情:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "✗ 数据导入失败" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "错误信息:" -ForegroundColor Red
    $_.Exception.Response.StatusCode | Write-Host
    $_.Exception.Message | Write-Host
    exit 1
}

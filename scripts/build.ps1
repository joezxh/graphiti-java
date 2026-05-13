# ============================================================
# Graphiti-Java 本地构建脚本 (Windows PowerShell)
# 用于在 Docker 打包前构建前后端
# ============================================================

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Green
Write-Host "  Graphiti-Java 本地构建" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

# 项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host ""
Write-Host "[1/3] 构建前端..." -ForegroundColor Yellow
Write-Host "-----------------------------------------" -ForegroundColor Yellow
Set-Location graphiti-web

if (-not (Test-Path "node_modules")) {
    Write-Host "安装前端依赖..." -ForegroundColor Cyan
    pnpm install
}

Write-Host "构建前端..." -ForegroundColor Cyan
pnpm build

# 创建后端静态资源目录
$StaticDir = "../graphiti-server/src/main/resources/static"
if (-not (Test-Path $StaticDir)) {
    New-Item -ItemType Directory -Force -Path $StaticDir | Out-Null
}

# 拷贝前端构建产物
Write-Host "拷贝前端产物到后端..." -ForegroundColor Cyan
Copy-Item -Recurse -Force dist/* $StaticDir

Set-Location $ProjectRoot

Write-Host ""
Write-Host "[2/3] 构建后端..." -ForegroundColor Yellow
Write-Host "-----------------------------------------" -ForegroundColor Yellow
Write-Host "Maven 构建 (跳过测试)..." -ForegroundColor Cyan
mvn clean package "-Dmaven.test.skip=true"

Write-Host ""
Write-Host "[3/3] 验证构建产物..." -ForegroundColor Yellow
Write-Host "-----------------------------------------" -ForegroundColor Yellow
$JarFiles = Get-ChildItem -Path "graphiti-server/target" -Filter "*.jar" -ErrorAction SilentlyContinue
if ($JarFiles) {
    $JarFile = $JarFiles[0]
    Write-Host "✓ 构建成功: $($JarFile.FullName)" -ForegroundColor Green
    $SizeMB = [math]::Round($JarFile.Length / 1MB, 2)
    Write-Host "  文件大小: $SizeMB MB" -ForegroundColor Gray
} else {
    Write-Host "✗ 构建失败: 未找到 JAR 文件" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  构建完成！" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "下一步：" -ForegroundColor Cyan
Write-Host "  启动 Docker: docker-compose up -d" -ForegroundColor White
Write-Host "  查看日志:    docker-compose logs -f graphiti-java" -ForegroundColor White
Write-Host ""

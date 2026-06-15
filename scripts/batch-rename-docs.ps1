#!/usr/bin/env pwsh
# 批量更新文档中的旧名称为 OntoGraph 新名称
# 使用方法: .\scripts\batch-rename-docs.ps1

$ErrorActionPreference = "Stop"

# 定义替换规则
$replacements = @{
    'graphiti-server' = 'ontograph-server'
    'graphiti-module-core' = 'ontograph-module-core'
    'graphiti-module-system' = 'ontograph-module-system'
    'graphiti-framework' = 'ontograph-framework'
    'graphiti-web' = 'ontograph-web'
    'graphiti-java' = 'ontograph-java'
    'Graphiti-Java' = 'OntoGraph'
    'Graphiti Java' = 'OntoGraph'
    'Graphiti框架' = 'OntoGraph框架'
    'Graphiti项目' = 'OntoGraph项目'
    'Graphiti系统' = 'OntoGraph系统'
    'Graphiti应用' = 'OntoGraph应用'
    'Graphiti后端' = 'OntoGraph后端'
    'Graphiti前端' = 'OntoGraph前端'
}
$files = Get-ChildItem -Path $PSScriptRoot\.. -Recurse -Filter "*.md" -Exclude "node_modules",".git","target"

$updatedCount = 0
$skippedCount = 0

foreach ($file in $files) {
    # 跳过 node_modules、.git、target 目录
    if ($file.FullName -match '\\node_modules\\|\\.git\\|\\target\\') {
        continue
    }
    
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $originalContent = $content
        
        # 应用所有替换规则
        foreach ($key in $replacements.Keys) {
            $content = $content -replace $key, $replacements[$key]
        }
        
        # 如果内容有变化,则写回文件
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            Write-Host "✓ Updated: $($file.Name)" -ForegroundColor Green
            $updatedCount++
        } else {
            $skippedCount++
        }
    }
    catch {
        Write-Host "✗ Error processing $($file.FullName): $_" -ForegroundColor Red
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "批量更新完成!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "更新文件数: $updatedCount" -ForegroundColor Green
Write-Host "跳过文件数: $skippedCount" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

$lines = Get-Content 'D:\projects\graphiti-java\graphiti-web\src\views\graph\ide.vue' | Select-Object -First 300
$count = 0
$openedAt = @()
$closedAt = @()

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $lineNum = $i + 1
    
    # Match opening div tags (but not self-closing or closing)
    if ($line -match '<div[^/\s>]' -and $line -notmatch '</div>' -and $line -notmatch '/>') {
        $count++
        $openedAt += "$lineNum"
    }
    
    # Match closing div tags
    if ($line -match '</div>') {
        $count--
        $closedAt += "$lineNum"
    }
    
    if ($count -lt 0) {
        Write-Output "Line ${lineNum}: NEGATIVE count = ${count}"
        Write-Output "  Line content: $line"
    }
}

Write-Output ""
Write-Output "Opened divs (first 50): $($openedAt[0..49] -join ', ')"
Write-Output "Total opened: $($openedAt.Count)"
Write-Output "Total closed: $($closedAt.Count)"
Write-Output "Final count: $count"

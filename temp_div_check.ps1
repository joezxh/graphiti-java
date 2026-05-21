$lines = Get-Content 'D:\projects\graphiti-java\graphiti-web\src\views\graph\ide.vue' | Select-Object -First 800
$count = 0
for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    if ($line -match '<div[^/]*[^\/]>') { $count++ }
    if ($line -match '</div>') { $count-- }
    if ($count -lt 0) {
        Write-Output "NEGATIVE at line $($i+1): $line"
        Write-Output "Count is now: $count"
    }
}
Write-Output "Final count: $count"

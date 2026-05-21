$lines = Get-Content 'D:\projects\graphiti-java\graphiti-web\src\views\graph\ide.vue'
for ($i = 785; $i -lt 801; $i++) {
    '{0:D4}|{1}' -f ($i+1), $lines[$i]
}

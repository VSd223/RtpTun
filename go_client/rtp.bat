@echo off
chcp 65001 >nul
set "OUTPUT_FILE=all_project_files.txt"

if exist "%OUTPUT_FILE%" del "%OUTPUT_FILE%"

echo Сборка содержимого файлов...

powershell -NoProfile -Command ^
    "$excludeDirs = @('.idea', '.git', 'output');" ^
    "$excludeFiles = @('universal-bypass-tool', '%OUTPUT_FILE%', 'gather_code.bat');" ^
    "$files = Get-ChildItem -Recurse -File | Where-Object {" ^
    "   $rel = $_.FullName.Substring((Get-Location).Path.Length + 1);" ^
    "   $inExcludedDir = $excludeDirs | Where-Object { $rel -like \"$_*\" };" ^
    "   -not $inExcludedDir -and ($excludeFiles -notcontains $_.Name) -and ($_.Length -lt 2097152)" ^
    "};" ^
    "foreach ($f in $files) {" ^
    "   $relPath = $f.FullName.Substring((Get-Location).Path.Length + 1);" ^
    "   '==================================================' | Out-File -Append -Encoding utf8 '%OUTPUT_FILE%';" ^
    "   \"FILE: $relPath\" | Out-File -Append -Encoding utf8 '%OUTPUT_FILE%';" ^
    "   '==================================================' | Out-File -Append -Encoding utf8 '%OUTPUT_FILE%';" ^
    "   try { Get-Content -Path $f.FullName -Raw -Encoding utf8 | Out-File -Append -Encoding utf8 '%OUTPUT_FILE%' } catch {} ;" ^
    "   \"`r`n`r`n\" | Out-File -Append -Encoding utf8 '%OUTPUT_FILE%';" ^
    "}"

echo Готово! Все файлы собраны в %OUTPUT_FILE%
pause
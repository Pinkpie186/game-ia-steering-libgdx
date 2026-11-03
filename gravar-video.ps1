param(
    [string]$Ffmpeg = 'ffmpeg',
    [string]$Saida = (Join-Path $PSScriptRoot 'Demonstracao-48s.mp4')
)
$ErrorActionPreference = 'Stop'
$javaExe = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
if (-not (Get-Command $javaExe -ErrorAction SilentlyContinue)) { throw 'Instale um JDK 17 e configure JAVA_HOME.' }
if (-not (Get-Command $Ffmpeg -ErrorAction SilentlyContinue)) { throw 'Instale o FFmpeg ou informe seu caminho com -Ffmpeg.' }
Push-Location $PSScriptRoot
try {
    & "$PSScriptRoot/gradlew.bat" lwjgl3:dist
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar.' }
    $frames = Join-Path $PSScriptRoot ('frames/captura-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    & $javaExe -jar "$PSScriptRoot/lwjgl3/build/libs/atividade-steering.jar" --record $frames
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gravar a aplicação.' }
    & $Ffmpeg -y -framerate 30 -i "$frames/frame-%05d.png" -c:v libx264 -crf 20 -pix_fmt yuv420p -movflags +faststart $Saida
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o MP4.' }
    Write-Host "Vídeo salvo em $Saida"
} finally { Pop-Location }

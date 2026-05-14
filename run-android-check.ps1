param(
    [string]$JavaHome = "C:\Program Files\Android\Android Studio\jbr",
    [string]$GradleUserHome = ""
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Android Studio JBR was not found: $JavaHome"
}

if ([string]::IsNullOrWhiteSpace($GradleUserHome)) {
    $GradleUserHome = Join-Path $PSScriptRoot ".gradle-local"
}

New-Item -ItemType Directory -Force -Path $GradleUserHome | Out-Null

$env:JAVA_HOME = $JavaHome
$env:GRADLE_USER_HOME = $GradleUserHome

Write-Host "Android project check"
Write-Host "- JAVA_HOME: $env:JAVA_HOME"
Write-Host "- GRADLE_USER_HOME: $env:GRADLE_USER_HOME"
Write-Host ""

cmd /c gradlew.bat help --offline

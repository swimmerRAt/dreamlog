param(
    [ValidateSet("test", "run", "smoke")]
    [string]$Mode = "smoke"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Resolve-JavaHome {
    $preferred = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path (Join-Path $preferred "bin\java.exe")) {
        return $preferred
    }

    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    throw "Java runtime was not found."
}

function Resolve-SqliteJar {
    $candidates = @(
        "C:\Program Files\Android\Android Studio\plugins\android\lib\sqlite-jdbc-3.51.1.0.jar",
        "C:\Users\USER\.gradle\caches\modules-2\files-2.1\org.xerial\sqlite-jdbc\3.41.2.2\ddeb8d3a3004f412ed19b4c98b3aec11d9430267\sqlite-jdbc-3.41.2.2.jar"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "sqlite-jdbc jar was not found."
}

function Compile-Backend {
    param(
        [string]$JavaHome,
        [string]$SqliteJar
    )

    $classesDir = Join-Path $PSScriptRoot "var\runtime\classes"
    New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

    & (Join-Path $JavaHome "bin\javac.exe") `
        --add-modules jdk.httpserver `
        -cp $SqliteJar `
        -d $classesDir `
        (Join-Path $PSScriptRoot "src\main\java\com\vibecoding\rental\AuthCrypto.java") `
        (Join-Path $PSScriptRoot "src\main\java\com\vibecoding\rental\SqliteAuthStore.java") `
        (Join-Path $PSScriptRoot "src\main\java\com\vibecoding\rental\RentalContractBackendApplication.java")

    if ($LASTEXITCODE -ne 0) {
        throw "Java backend compilation failed."
    }
}

function Start-BackendProcess {
    param(
        [string]$JavaHome,
        [string]$SqliteJar
    )

    $classesDir = Join-Path $PSScriptRoot "var\runtime\classes"
    $classpath = "$classesDir;$SqliteJar"

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = Join-Path $JavaHome "bin\java.exe"
    $psi.Arguments = "--add-modules jdk.httpserver -cp `"$classpath`" com.vibecoding.rental.RentalContractBackendApplication"
    $psi.WorkingDirectory = $PSScriptRoot
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $null = $process.Start()
    return $process
}

function Wait-ForHealth {
    param([int]$TimeoutSeconds = 20)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            return Invoke-RestMethod -Uri "http://localhost:3000/api/health" -TimeoutSec 3
        }
        catch {
            Start-Sleep -Seconds 1
        }
    }

    throw "The backend did not become healthy in time."
}

function Invoke-SmokeCheck {
    param(
        [string]$JavaHome,
        [string]$SqliteJar
    )

    $process = Start-BackendProcess -JavaHome $JavaHome -SqliteJar $SqliteJar
    try {
        $health = Wait-ForHealth
        $login = Invoke-RestMethod -Uri "http://localhost:3000/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"demo@example.com","password":"demo1234"}'
        $headers = @{ Authorization = "Bearer $($login.access_token)" }
        $me = Invoke-RestMethod -Uri "http://localhost:3000/api/auth/me" -Headers $headers -TimeoutSec 5

        Write-Host ""
        Write-Host "Smoke check result"
        Write-Host "- Status: $($health.data.status)"
        Write-Host "- Service: $($health.data.service)"
        Write-Host "- Demo account: $($me.email)"
    }
    finally {
        if (-not $process.HasExited) {
            $process.Kill()
            $process.WaitForExit()
        }
    }
}

$javaHome = Resolve-JavaHome
$sqliteJar = Resolve-SqliteJar

switch ($Mode) {
    "test" {
        Compile-Backend -JavaHome $javaHome -SqliteJar $sqliteJar
        Write-Host ""
        Write-Host "Compilation succeeded."
    }
    "run" {
        Compile-Backend -JavaHome $javaHome -SqliteJar $sqliteJar
        Write-Host ""
        Write-Host "Starting the backend. Press Ctrl + C to stop."
        & (Join-Path $javaHome "bin\java.exe") --add-modules jdk.httpserver -cp "$(Join-Path $PSScriptRoot 'var\runtime\classes');$sqliteJar" com.vibecoding.rental.RentalContractBackendApplication
    }
    "smoke" {
        Compile-Backend -JavaHome $javaHome -SqliteJar $sqliteJar
        Invoke-SmokeCheck -JavaHome $javaHome -SqliteJar $sqliteJar
    }
}

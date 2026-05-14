param(
    [ValidateSet("test", "run", "smoke")]
    [string]$Mode = "smoke",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    $fallback = "C:\Users\USER\.jdks\openjdk-25.0.1"
    if (Test-Path (Join-Path $fallback "bin\java.exe")) {
        return $fallback
    }

    throw "JAVA_HOME was not found. Check your environment variable or fallback path."
}

function Resolve-MavenCommand {
    $fromPath = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    if ($env:MAVEN_HOME) {
        $fromEnv = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
        if (Test-Path $fromEnv) {
            return $fromEnv
        }
    }

    $fallback = "C:\Users\USER\.vscode\extensions\oracle.oracle-java-25.1.0\nbcode\java\maven\bin\mvn.cmd"
    if (Test-Path $fallback) {
        return $fallback
    }

    throw "Maven command was not found. Check MAVEN_HOME or the fallback path."
}

function Resolve-MavenRepo {
    $repo = "C:\Users\USER\.m2\repository"
    if (Test-Path $repo) {
        return $repo
    }

    throw "Maven local repository was not found: $repo"
}

function Invoke-OfflineTests {
    param(
        [string]$MavenCommand,
        [string]$MavenRepo
    )

    Write-Host ""
    Write-Host "[1/2] Running offline tests..."

    Push-Location $PSScriptRoot
    $env:MAVEN_OPTS = "-Dmaven.repo.local=$MavenRepo"
    try {
        & $MavenCommand -o test
    }
    finally {
        Pop-Location
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Tests failed."
    }
}

function Get-RuntimeClasspath {
    $classpathFile = Join-Path $PSScriptRoot "var\runtime\java-test-classpath.txt"
    if (-not (Test-Path $classpathFile)) {
        throw "Runtime classpath file is missing: $classpathFile"
    }

    return (Get-Content $classpathFile -Raw).Trim()
}

function Wait-ForHealth {
    param([int]$TimeoutSeconds = 30)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            return Invoke-RestMethod -Uri "http://localhost:3000/api/health" -TimeoutSec 3
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "The server did not become ready within the timeout."
}

function Start-AppProcess {
    param(
        [string]$JavaHome,
        [string]$Classpath
    )

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = Join-Path $JavaHome "bin\java.exe"
    $psi.Arguments = "-cp `"$Classpath`" com.vibecoding.rental.RentalContractBackendApplication"
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

function Invoke-SmokeCheck {
    param([string]$JavaHome)

    Write-Host ""
    Write-Host "[2/2] Starting the app briefly and checking key endpoints..."

    $classpath = Get-RuntimeClasspath
    $process = Start-AppProcess -JavaHome $JavaHome -Classpath $classpath

    try {
        $health = Wait-ForHealth
        $template = Invoke-RestMethod -Uri "http://localhost:3000/api/templates/default" -TimeoutSec 5
        $knowledge = Invoke-RestMethod -Uri "http://localhost:3000/api/knowledge/overview" -TimeoutSec 5

        Write-Host ""
        Write-Host "Smoke check result"
        Write-Host "- Status: $($health.data.status)"
        Write-Host "- Service: $($health.data.service)"
        Write-Host "- Template: $($template.data.id)"
        Write-Host "- Knowledge sections: $(@($knowledge.data.PSObject.Properties.Name) -join ', ')"
    }
    finally {
        if (-not $process.HasExited) {
            $process.Kill()
            $process.WaitForExit()
        }
    }
}

$javaHome = Resolve-JavaHome
$mavenCommand = Resolve-MavenCommand
$mavenRepo = Resolve-MavenRepo
$env:JAVA_HOME = $javaHome
Set-Location $PSScriptRoot

switch ($Mode) {
    "test" {
        Invoke-OfflineTests -MavenCommand $mavenCommand -MavenRepo $mavenRepo
        Write-Host ""
        Write-Host "All tests passed."
    }
    "run" {
        if (-not $SkipTests) {
            Invoke-OfflineTests -MavenCommand $mavenCommand -MavenRepo $mavenRepo
        }

        $classpath = Get-RuntimeClasspath
        Write-Host ""
        Write-Host "Starting the server. Press Ctrl + C to stop."
        & (Join-Path $javaHome "bin\java.exe") -cp $classpath com.vibecoding.rental.RentalContractBackendApplication
    }
    "smoke" {
        if (-not $SkipTests) {
            Invoke-OfflineTests -MavenCommand $mavenCommand -MavenRepo $mavenRepo
        }

        Invoke-SmokeCheck -JavaHome $javaHome
    }
}

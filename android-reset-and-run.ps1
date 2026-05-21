$ErrorActionPreference = "Stop"

$sdkRoot = "C:\Users\USER\AppData\Local\Android\Sdk"
$avdHome = "C:\Users\USER\.android\avd"
$avdName = "Medium_Phone_API_36.1"
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$emulator = Join-Path $sdkRoot "emulator\emulator.exe"

$env:ANDROID_SDK_ROOT = $sdkRoot
$env:ANDROID_AVD_HOME = $avdHome
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "Resetting adb and old emulator processes..."
& $adb kill-server | Out-Null
Get-Process adb,emulator,qemu-system-x86_64 -ErrorAction SilentlyContinue | Stop-Process -Force

$avdPath = Join-Path $avdHome "Medium_Phone.avd"
$cleanupTargets = @(
    (Join-Path $avdPath "snapshots"),
    (Join-Path $avdPath "cache.img"),
    (Join-Path $avdPath "cache.img.qcow2"),
    (Join-Path $avdPath "userdata-qemu.img"),
    (Join-Path $avdPath "userdata-qemu.img.qcow2"),
    (Join-Path $avdPath "sdcard.img"),
    (Join-Path $avdPath "sdcard.img.qcow2")
)

foreach ($target in $cleanupTargets) {
    if (Test-Path $target) {
        Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Starting emulator: $avdName"
Start-Process -FilePath $emulator -ArgumentList "-avd $avdName -no-snapshot-load -wipe-data"

Write-Host "Waiting for adb connection..."
& $adb start-server | Out-Null
$deadline = (Get-Date).AddMinutes(3)
do {
    Start-Sleep -Seconds 5
    $devices = & $adb devices
    if ($devices -match "emulator-\d+\s+device") {
        Write-Host ""
        Write-Host "Emulator is connected."
        & $adb devices
        exit 0
    }
} while ((Get-Date) -lt $deadline)

Write-Host ""
Write-Host "Emulator did not reach adb device state within 3 minutes."
& $adb devices
exit 1

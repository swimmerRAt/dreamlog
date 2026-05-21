param(
    [switch]$HealthOnly
)

$workspaceRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$shimRoot = Join-Path $workspaceRoot "server_shims"
$serverRoot = "C:\Users\USER\Desktop\dreamlog\server"

if (-not (Test-Path $serverRoot)) {
    throw "Dreamlog server folder not found: $serverRoot"
}

$pythonPathEntries = @($shimRoot, $serverRoot)
if ($env:PYTHONPATH) {
    $pythonPathEntries += $env:PYTHONPATH
}
$env:PYTHONPATH = ($pythonPathEntries -join ";")

Push-Location $serverRoot
try {
    if ($HealthOnly) {
        @'
import asyncio
import json
from main import health

async def run():
    result = await health()
    print(json.dumps(result, ensure_ascii=False))

asyncio.run(run())
'@ | python -
        exit $LASTEXITCODE
    }

    python -m uvicorn main:app --host 0.0.0.0 --port 8000
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}


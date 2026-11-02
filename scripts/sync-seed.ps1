# Copy canonical seed into Android assets, unit-test resources, and web/.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "data\encounters.json"
if (-not (Test-Path $src)) { throw "Missing $src" }

$webDir = Join-Path $root "web"
if (-not (Test-Path $webDir)) { New-Item -ItemType Directory -Path $webDir | Out-Null }

Copy-Item -Force $src (Join-Path $root "android\app\src\main\assets\encounters.json")
Copy-Item -Force $src (Join-Path $root "android\app\src\test\resources\encounters.json")
Copy-Item -Force $src (Join-Path $webDir "encounters.json")
& (Join-Path $PSScriptRoot "embed-web-seed.ps1")
Write-Host "Synced encounters.json -> android assets + test resources + web/"

# Copy canonical seed into Android assets + unit-test resources.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "data\encounters.json"
if (-not (Test-Path $src)) { throw "Missing $src" }
Copy-Item -Force $src (Join-Path $root "android\app\src\main\assets\encounters.json")
Copy-Item -Force $src (Join-Path $root "android\app\src\test\resources\encounters.json")
Write-Host "Synced encounters.json -> assets + test resources"

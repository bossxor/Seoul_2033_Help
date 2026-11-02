# Serve web/ on http://127.0.0.1:8080 (optional; file:// also works after sync-seed).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
Write-Host "http://127.0.0.1:8080  (Ctrl+C to stop)"
python -m http.server 8080 --bind 127.0.0.1

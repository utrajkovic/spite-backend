param(
  [Parameter(Mandatory = $true)][string]$SourceDumpPath,
  [Parameter(Mandatory = $true)][string]$RestoreMongoUri
)

if (-not (Test-Path $SourceDumpPath)) {
  Write-Error "Dump path not found: $SourceDumpPath"
  exit 1
}

Write-Host "Running restore test from $SourceDumpPath"
mongorestore --uri="$RestoreMongoUri" --drop "$SourceDumpPath"

if ($LASTEXITCODE -ne 0) {
  Write-Error "Restore test failed"
  exit 1
}

Write-Host "Restore test completed successfully"
exit 0

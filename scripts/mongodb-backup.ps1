param(
  [Parameter(Mandatory = $true)][string]$MongoUri,
  [Parameter(Mandatory = $true)][string]$BackupRoot
)

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm"
$targetDir = Join-Path $BackupRoot "spite_backup_$timestamp"
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

Write-Host "Creating backup in $targetDir"
mongodump --uri="$MongoUri" --out="$targetDir"

if ($LASTEXITCODE -ne 0) {
  Write-Error "mongodump failed"
  exit 1
}

Write-Host "Backup completed: $targetDir"
exit 0

# Mongo Backup & Restore Runbook

## Daily backup
- Run once per day (Task Scheduler / CI cron):
- `powershell -ExecutionPolicy Bypass -File .\scripts\mongodb-backup.ps1 -MongoUri "$env:SPRING_DATA_MONGODB_URI" -BackupRoot "D:\\spite-backups"`
- Keep at least 14 daily backups.

## Weekly restore verification
- Prepare a separate restore/test database URI.
- Run once per week:
- `powershell -ExecutionPolicy Bypass -File .\scripts\mongodb-restore-test.ps1 -SourceDumpPath "D:\\spite-backups\\spite_backup_YYYY-MM-DD_HH-mm" -RestoreMongoUri "$env:SPITE_RESTORE_TEST_MONGO_URI"`
- Verify key collections exist: `users`, `workouts`, `exercises`, `workout_feedback`, `completed_workouts`, `assigned_workouts`.

## Operational notes
- Never run restore test against production database.
- Store Mongo URIs in environment variables/secrets, never in source code.
- Alert on backup/restore script non-zero exit code.

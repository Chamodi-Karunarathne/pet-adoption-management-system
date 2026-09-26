$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$dataDir = Join-Path $root '.local/postgres'
$bin = Split-Path (Get-Command psql.exe).Source
& (Join-Path $bin 'pg_ctl.exe') -D $dataDir -m fast -w stop
if ($LASTEXITCODE -ne 0) { throw 'Local database could not be stopped' }

param([int]$Port = 55432, [string]$PostgresBin = '')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$configPath = Join-Path $root 'config/application.properties'
if (Test-Path $configPath) {
    $existingConfig = [IO.File]::ReadAllText($configPath)
    if ($existingConfig -notmatch '(?m)^db.url=jdbc:postgresql://127\.0\.0\.1:\d+/petadoption_review\r?$') {
        throw 'An existing non-review configuration was found. Preserve it before running local review setup.'
    }
}
if (!$PostgresBin) { $PostgresBin = Split-Path (Get-Command psql.exe).Source }
$localDir = Join-Path $root '.local'
$dataDir = Join-Path $localDir 'postgres'
New-Item -ItemType Directory -Force $localDir,(Join-Path $root 'config') | Out-Null
$secretFile = Join-Path $localDir 'db-secret.txt'
if (!(Test-Path $secretFile)) {
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    [IO.File]::WriteAllText($secretFile,[Convert]::ToBase64String($bytes))
}
$dbSecret = [IO.File]::ReadAllText($secretFile)
if (!(Test-Path (Join-Path $dataDir 'PG_VERSION'))) {
    & (Join-Path $PostgresBin 'initdb.exe') -D $dataDir -U pet_local_owner --pwfile=$secretFile --auth=scram-sha-256 --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'initdb failed' }
    Add-Content (Join-Path $dataDir 'postgresql.conf') "`nlisten_addresses = '127.0.0.1'`nport = $Port"
}
& (Join-Path $PostgresBin 'pg_ctl.exe') -D $dataDir status *> $null
if ($LASTEXITCODE -ne 0) {
    $start = Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') -ArgumentList @('-D',('"' + $dataDir + '"'),'-l',('"' + (Join-Path $localDir 'postgres.log') + '"'),'-w','start') -WindowStyle Hidden -PassThru
    $start.WaitForExit()
    if ($start.ExitCode -ne 0) { throw 'PostgreSQL start failed; see .local/postgres.log' }
}
$oldPgPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $dbSecret
    foreach ($dbName in @('petadoption_review','petadoption_test')) {
        $exists = & (Join-Path $PostgresBin 'psql.exe') -h 127.0.0.1 -p $Port -U pet_local_owner -d postgres -tAc "SELECT count(*) FROM pg_database WHERE datname='$dbName'"
        if ($LASTEXITCODE -ne 0) { throw 'Cannot connect to local PostgreSQL' }
        if ($exists -ne '1') {
            & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $Port -U pet_local_owner $dbName
            if ($LASTEXITCODE -ne 0) { throw 'Database creation failed' }
        }
    }
} finally { $env:PGPASSWORD = $oldPgPassword }
$config = "db.url=jdbc:postgresql://127.0.0.1:$Port/petadoption_review`ndb.user=pet_local_owner`ndb.password=$dbSecret`napp.currency=LKR`n"
[IO.File]::WriteAllText((Join-Path $root 'config/application.properties'),$config)
Write-Output "Local PostgreSQL ready on port $Port. Review and test databases are isolated from your installed service."

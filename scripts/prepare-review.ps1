$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & (Join-Path $PSScriptRoot 'start-local-db.ps1')
    & mvn -B package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    $jar = Join-Path $root 'target/PetAdoption-1.0-SNAPSHOT.jar'
    & java -jar $jar --migrate
    if ($LASTEXITCODE -ne 0) { throw 'Schema migration failed' }
    $credentials = Join-Path $root '.local/review-credentials.txt'
    if (!(Test-Path $credentials)) {
        $bytes = New-Object byte[] 18
        [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
        $reviewPassword = [Convert]::ToBase64String($bytes)
        $env:PET_DEMO_PASSWORD = $reviewPassword
        try {
            & java -jar $jar --demo
            if ($LASTEXITCODE -ne 0) { throw 'Demo installation failed (only an empty database is accepted)' }
            [IO.File]::WriteAllText($credentials,"LOCAL REVIEW ONLY`nAdmin: admin@woof.example`nAdopter: adopter@woof.example`nPassword for both: $reviewPassword`n")
        } finally { Remove-Item Env:PET_DEMO_PASSWORD -ErrorAction SilentlyContinue }
    }
    Write-Output 'Review setup complete. Credentials are in .local/review-credentials.txt (ignored).'
} finally { Pop-Location }

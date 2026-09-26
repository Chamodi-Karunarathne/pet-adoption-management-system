$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $root 'target/PetAdoption-1.0-SNAPSHOT.jar'
if (!(Test-Path $jar)) { throw 'Build first: mvn package (or scripts/prepare-review.ps1).' }
Start-Process -FilePath 'javaw.exe' -WorkingDirectory $root -ArgumentList @('-jar',('"' + $jar + '"')) -PassThru

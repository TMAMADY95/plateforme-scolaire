# Charge une ou plusieurs étapes de la simulation du baccalauréat dans le conteneur Docker du projet (Windows PowerShell).
#   .\charger_etapes.ps1                 charge les étapes 1 à 9
#   .\charger_etapes.ps1 -De 1 -A 3      charge les étapes 1 à 3
#   .\charger_etapes.ps1 -De 4 -A 4      charge uniquement l'étape 4
param([int]$De = 1, [int]$A = 9, [string]$Conteneur = 'scolaire-db')
$ErrorActionPreference = 'Stop'
for ($i = $De; $i -le $A; $i++) {
    $fichier = Get-ChildItem -Path $PSScriptRoot -Filter ('bac_etape_{0:D2}_*.sql' -f $i) | Select-Object -First 1
    if (-not $fichier) { throw "Étape $i introuvable dans $PSScriptRoot" }
    Write-Host "Étape ${i} : $($fichier.Name)"
    docker cp $fichier.FullName "${Conteneur}:/tmp/$($fichier.Name)"
    docker exec -e PGCLIENTENCODING=UTF8 $Conteneur psql -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f "/tmp/$($fichier.Name)"
    if ($LASTEXITCODE -ne 0) { throw "L'étape $i a échoué : rien n'a été enregistré pour cette étape." }
}

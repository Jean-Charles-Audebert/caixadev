# Charge le .env et démarre le backend Spring Boot
# Usage : .\start-dev.ps1

$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Error ".env introuvable. Copiez .env.example en .env et renseignez les valeurs."
    exit 1
}

# Charger les variables du .env dans la session courante
Get-Content $envFile | Where-Object { $_ -match "^\s*[^#].*=.*" } | ForEach-Object {
    $parts = $_ -split "=", 2
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    Write-Host "  $key chargée"
}

Write-Host ""
Write-Host "Démarrage du backend Spring Boot (profil : $env:SPRING_PROFILES_ACTIVE)..."
.\mvnw spring-boot:run


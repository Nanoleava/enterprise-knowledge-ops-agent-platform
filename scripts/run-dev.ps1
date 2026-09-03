[CmdletBinding()]
param(
    [switch]$PrepareOnly
)

$ErrorActionPreference = "Stop"

function Test-JwtSecretBase64 {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }

    try {
        $decoded = [Convert]::FromBase64String($Value.Trim())
        return $decoded.Length -ge 32
    }
    catch {
        return $false
    }
}

$env:SPRING_PROFILES_ACTIVE = "dev"

if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "DB_PASSWORD is missing. Set it before running this script."
}

if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    $env:DB_USERNAME = "root"
}

if ([string]::IsNullOrWhiteSpace($env:JWT_ISSUER)) {
    $env:JWT_ISSUER = "https://enterprise-agent-platform.local"
}

if ([string]::IsNullOrWhiteSpace($env:JWT_ACCESS_TOKEN_TTL)) {
    $env:JWT_ACCESS_TOKEN_TTL = "30m"
}

if (-not (Test-JwtSecretBase64 -Value $env:JWT_SECRET_BASE64)) {
    $secretBytes = New-Object byte[] 32
    $randomNumberGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomNumberGenerator.GetBytes($secretBytes)
    }
    finally {
        $randomNumberGenerator.Dispose()
    }
    $env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($secretBytes)
    Write-Host "JWT_SECRET_BASE64 was missing, invalid, or too short. Generated a 32-byte key for this process."
}

if ($PrepareOnly) {
    Write-Host "Development environment validation passed. Application was not started."
    return
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$mavenExitCode = 0

Push-Location -LiteralPath $projectRoot
try {
    & mvn spring-boot:run
    $mavenExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($mavenExitCode -ne 0) {
    throw "mvn spring-boot:run failed with exit code $mavenExitCode."
}

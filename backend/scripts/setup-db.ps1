# Script to verify and set up the PostgreSQL database without exposing credentials
param (
    [string]$DbHost = $env:DB_HOST,
    [string]$DbPort = $env:DB_PORT,
    [string]$DbName = $env:DB_NAME,
    [string]$DbUser = $env:DB_USERNAME
)

if ([string]::IsNullOrWhiteSpace($DbHost)) { $DbHost = "localhost" }
if ([string]::IsNullOrWhiteSpace($DbPort)) { $DbPort = "5432" }
if ([string]::IsNullOrWhiteSpace($DbName)) { $DbName = "network_management" }
if ([string]::IsNullOrWhiteSpace($DbUser)) { $DbUser = "postgres" }

# Load from .env if present
$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim()
            if ($k -eq "DB_PASSWORD" -and [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
                $env:DB_PASSWORD = $v
            } elseif ($k -eq "DB_HOST" -and [string]::IsNullOrWhiteSpace($env:DB_HOST)) {
                $DbHost = $v
            } elseif ($k -eq "DB_PORT" -and [string]::IsNullOrWhiteSpace($env:DB_PORT)) {
                $DbPort = $v
            } elseif ($k -eq "DB_NAME" -and [string]::IsNullOrWhiteSpace($env:DB_NAME)) {
                $DbName = $v
            } elseif ($k -eq "DB_USERNAME" -and [string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
                $DbUser = $v
            }
        }
    }
}

# If still not in process env, check User environment variable
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    $userPwd = [System.Environment]::GetEnvironmentVariable("DB_PASSWORD", "User")
    if (-not [string]::IsNullOrWhiteSpace($userPwd)) {
        $env:DB_PASSWORD = $userPwd
    }
}

if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    Write-Error "DB_PASSWORD is not set. Please configure DB_PASSWORD in backend\.env or as a User environment variable."
    exit 1
}

# Locate psql
$psqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psqlPath)) {
    $cmd = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        $psqlPath = $cmd.Source
    } else {
        Write-Error "psql.exe not found at standard location or in PATH."
        exit 1
    }
}

$env:PGPASSWORD = $env:DB_PASSWORD

Write-Host "Checking if database '$DbName' exists on ${DbHost}:${DbPort}..."
$checkQuery = "SELECT 1 FROM pg_database WHERE datname = '$DbName';"
$dbExistsOutput = & $psqlPath -h $DbHost -p $DbPort -U $DbUser -d postgres -t -A -c $checkQuery 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to connect to PostgreSQL server: $dbExistsOutput"
    exit 2
}

if ($null -ne $dbExistsOutput -and "$dbExistsOutput".Trim() -eq "1") {
    Write-Host "Database '$DbName' already exists."
} else {
    Write-Host "Database '$DbName' does not exist. Creating database..."
    $createOutput = & $psqlPath -h $DbHost -p $DbPort -U $DbUser -d postgres -c "CREATE DATABASE $DbName;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Database '$DbName' successfully created."
    } else {
        Write-Error "Failed to create database '$DbName': $createOutput"
        exit 3
    }
}

# Verify connection to target database
Write-Host "Verifying connection to database '$DbName'..."
$testDb = & $psqlPath -h $DbHost -p $DbPort -U $DbUser -d $DbName -t -A -c "SELECT current_database();" 2>&1
if ($LASTEXITCODE -eq 0 -and $testDb.Trim() -eq $DbName) {
    Write-Host "Successfully verified connection to database '$DbName'."
    exit 0
} else {
    Write-Error "Connection verification failed: $testDb"
    exit 4
}

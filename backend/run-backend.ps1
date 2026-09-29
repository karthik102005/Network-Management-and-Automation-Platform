# Script to launch the Spring Boot backend with environment variables
param (
    [switch]$Jar
)

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim()
            if (-not [string]::IsNullOrWhiteSpace($k) -and [string]::IsNullOrWhiteSpace([System.Environment]::GetEnvironmentVariable($k, "Process"))) {
                [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
            }
        }
    }
}

# If DB_PASSWORD not in process, check user environment
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    $userPwd = [System.Environment]::GetEnvironmentVariable("DB_PASSWORD", "User")
    if (-not [string]::IsNullOrWhiteSpace($userPwd)) {
        $env:DB_PASSWORD = $userPwd
    }
}

if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    Write-Warning "DB_PASSWORD is not set. Spring Boot will attempt connecting using empty password."
}

if ($Jar) {
    $jarPath = Join-Path $PSScriptRoot "target\network-management-backend-1.0.0-SNAPSHOT.jar"
    if (-not (Test-Path $jarPath)) {
        Write-Host "Building JAR..."
        mvn clean package -DskipTests
    }
    Write-Host "Starting Spring Boot JAR on port 8080..."
    java -jar $jarPath
} else {
    Write-Host "Starting Spring Boot via Maven on port 8080..."
    mvn spring-boot:run
}

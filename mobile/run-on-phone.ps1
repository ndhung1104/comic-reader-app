param(
    [string]$Serial,
    [int]$Port = 8080,
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [string]$PackageName = "com.group09.ComicReader"
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Args
    )

    if ([string]::IsNullOrWhiteSpace($script:ResolvedSerial)) {
        & adb @Args
    } else {
        & adb -s $script:ResolvedSerial @Args
    }

    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Args -join ' ') failed with exit code $LASTEXITCODE."
    }
}

function Require-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found in PATH."
    }
}

Require-Command -Name "adb"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleWrapper = Join-Path $scriptDir "gradlew.bat"
$apkPath = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $gradleWrapper)) {
    throw "Could not find Gradle wrapper at '$gradleWrapper'."
}

if ($Port -lt 1 -or $Port -gt 65535) {
    throw "Port must be in range 1..65535."
}

& adb start-server | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start adb server."
}

$devicesOutput = & adb devices
if ($LASTEXITCODE -ne 0) {
    throw "Failed to list adb devices."
}

$onlineDevices = @(
    $devicesOutput |
    Select-Object -Skip 1 |
    Where-Object { $_ -match "^\S+\s+device$" } |
    ForEach-Object { ($_ -split "\s+")[0] }
)

if ([string]::IsNullOrWhiteSpace($Serial)) {
    if ($onlineDevices.Count -eq 0) {
        throw "No online Android device detected. Connect a phone and enable USB debugging."
    }
    if ($onlineDevices.Count -gt 1) {
        throw "Multiple devices detected ($($onlineDevices -join ', ')). Re-run with -Serial <deviceId>."
    }
    $script:ResolvedSerial = $onlineDevices[0]
} else {
    if (-not ($onlineDevices -contains $Serial)) {
        throw "Device '$Serial' is not online. Available: $($onlineDevices -join ', ')."
    }
    $script:ResolvedSerial = $Serial
}

Write-Host "Using device: $script:ResolvedSerial"
Invoke-Adb -Args @("wait-for-device")

Write-Host "Setting port reverse tcp:$Port -> tcp:$Port"
Invoke-Adb -Args @("reverse", "tcp:$Port", "tcp:$Port")

if (-not $SkipBuild) {
    Write-Host "Building debug APK..."
    Push-Location $scriptDir
    try {
        & $gradleWrapper assembleDebug
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "Skipping build (-SkipBuild)."
}

if (-not (Test-Path $apkPath)) {
    throw "APK not found at '$apkPath'. Run without -SkipBuild or build manually first."
}

Write-Host "Installing APK: $apkPath"
Invoke-Adb -Args @("install", "-r", $apkPath)

if (-not $NoLaunch) {
    Write-Host "Launching app: $PackageName"
    Invoke-Adb -Args @("shell", "monkey", "-p", $PackageName, "-c", "android.intent.category.LAUNCHER", "1")
} else {
    Write-Host "Skipping launch (-NoLaunch)."
}

Write-Host ""
Write-Host "Done."
Write-Host "If your backend runs on this PC, set MOBILE_BASE_URL=http://127.0.0.1:$Port/ in mobile/.env."

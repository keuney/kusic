param(
    [string]$Serial = 'emulator-5554',
    [string]$Adb = "$env:ANDROID_HOME/platform-tools/adb.exe"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$outputDirectory = Join-Path $projectRoot 'captures/km-012'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

function Invoke-Adb {
    param([string[]]$Arguments)
    $result = & $Adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ADB 실패: $($Arguments -join ' ')"
    }
    return $result
}

Invoke-Adb -Arguments @('install', '-r', (Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk'))
Invoke-Adb -Arguments @('shell', 'am', 'force-stop', 'com.keuney.music')
$launch = Invoke-Adb -Arguments @('shell', 'am', 'start', '-W', '-n', 'com.keuney.music/.MainActivity')
if (($launch -join "`n") -notmatch 'Status: ok') {
    throw '런처 Activity 실행 실패'
}
$launch

$remoteHierarchy = '/data/local/tmp/keuney-km012.xml'
$remoteScreenshot = '/data/local/tmp/keuney-km012.png'
try {
    Invoke-Adb -Arguments @('shell', 'uiautomator', 'dump', $remoteHierarchy)
    Invoke-Adb -Arguments @('pull', $remoteHierarchy, (Join-Path $outputDirectory 'hierarchy.xml'))
    [xml]$hierarchy = Get-Content -Raw (Join-Path $outputDirectory 'hierarchy.xml')
    $placeholder = $hierarchy.SelectSingleNode('//node[@text="Keuney Music" and @package="com.keuney.music"]')
    if ($null -eq $placeholder) {
        throw 'Keuney Music 기본 문구를 화면에서 찾지 못함'
    }
    Invoke-Adb -Arguments @('shell', 'pidof', 'com.keuney.music')
    Invoke-Adb -Arguments @('shell', 'screencap', '-p', $remoteScreenshot)
    Invoke-Adb -Arguments @('pull', $remoteScreenshot, (Join-Path $outputDirectory 'launch.png'))
    Write-Output "PASS: 앱 실행 및 Keuney Music 표시 확인 ($($placeholder.bounds))"
} finally {
    Invoke-Adb -Arguments @('shell', 'rm', '-f', $remoteHierarchy, $remoteScreenshot)
}

<#
.SYNOPSIS
    Arranca, apaga y opera el emulador Android del proyecto Notally sin
    depender de Android Studio. Compatible con Windows PowerShell 5.1 y
    PowerShell 7.

.DESCRIPTION
    Detecta el SDK de Android en este orden:
      1. $env:ANDROID_HOME
      2. $env:ANDROID_SDK_ROOT
      3. sdk.dir de <repo>\local.properties (se normaliza el valor escapado,
         por ejemplo "D\:\\Sdk-Android" queda como "D:\Sdk-Android")
      4. Fallback documentado: D:\Sdk-Android

    Valida que existan emulator\emulator.exe y platform-tools\adb.exe.
    Todos los mensajes son ASCII (sin acentos ni emojis) para evitar
    problemas de codepage en PowerShell 5.1.

.PARAMETER Action
    run     (default) Arranca el emulador, espera el boot, instala el APK y lanza la app.
    kill    Apaga todos los emuladores activos (adb emu kill).
    list    Lista AVDs disponibles y dispositivos conectados.
    install Instala y lanza el APK en el dispositivo ya encendido.
    logs    Muestra logcat filtrado por el PID de la app (o logcat general).

.PARAMETER Avd
    Nombre del AVD a usar. Default: Pixel_7a

.PARAMETER Apk
    Ruta del APK, absoluta o relativa a la raiz del repo.
    Default: app\build\outputs\apk\debug\app-debug.apk

.EXAMPLE
    .\tools\emulador.ps1
    Arranca Pixel_7a, espera el boot, instala el APK debug y abre la app.

.EXAMPLE
    .\tools\emulador.ps1 -Action install -Avd Pixel_7a
    Instala y lanza el APK sobre el emulador que ya esta encendido.

.EXAMPLE
    .\tools\emulador.ps1 -Action logs
    Sigue el logcat del proceso com.example.notally.

.NOTES
    Acciones disponibles: run, kill, list, install, logs.
    Salida con exit 1 ante cualquier error.
#>

[CmdletBinding()]
param(
    [ValidateSet('run', 'kill', 'list', 'install', 'logs')]
    [string]$Action = 'run',

    [string]$Avd = 'Pixel_7a',

    [string]$Apk = 'app\build\outputs\apk\debug\app-debug.apk'
)

$ErrorActionPreference = 'Stop'

$PackageName   = 'com.example.notally'
$LaunchComp    = 'com.example.notally/.MainActivity'
$FallbackSdk   = 'D:\Sdk-Android'
$BootTimeout   = 180
$DeviceTimeout = 120

# ---------------------------------------------------------------------------
# Helpers de salida
# ---------------------------------------------------------------------------
function Write-Ok   { param([string]$m) Write-Host "[OK] $m" -ForegroundColor Green }
function Write-Step { param([string]$m) Write-Host "[..] $m" -ForegroundColor Cyan }
function Write-Warn { param([string]$m) Write-Host "[!!] $m" -ForegroundColor Yellow }
function Write-Err  { param([string]$m) Write-Host "[XX] $m" -ForegroundColor Red }
function Stop-Script {
    param([string]$m)
    Write-Err $m
    exit 1
}

# ---------------------------------------------------------------------------
# Raiz del repo y resolucion del SDK
# ---------------------------------------------------------------------------
$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) { $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition }
$RepoRoot = Split-Path -Parent $ScriptDir

function Resolve-SdkPath {
    $candidates = New-Object System.Collections.Generic.List[string]

    if ($env:ANDROID_HOME -and $env:ANDROID_HOME.Trim()) {
        $candidates.Add($env:ANDROID_HOME.Trim())
    }
    if ($env:ANDROID_SDK_ROOT -and $env:ANDROID_SDK_ROOT.Trim()) {
        $candidates.Add($env:ANDROID_SDK_ROOT.Trim())
    }

    $localProps = Join-Path $RepoRoot 'local.properties'
    if (Test-Path -LiteralPath $localProps) {
        $line = Get-Content -LiteralPath $localProps |
            Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
            Select-Object -First 1
        if ($line) {
            $raw = ($line -split '=', 2)[1].Trim()
            # Quita comillas si las hubiera
            $raw = $raw.Trim('"').Trim("'")
            # Normaliza el valor escapado de Java properties:
            #   D\:\\Sdk-Android  ->  D:\Sdk-Android
            $raw = $raw.Replace('\:', ':').Replace('\\', '\')
            if ($raw) { $candidates.Add($raw) }
        }
    }

    $candidates.Add($FallbackSdk)

    foreach ($cand in $candidates) {
        if ([string]::IsNullOrWhiteSpace($cand)) { continue }
        $emu = Join-Path $cand 'emulator\emulator.exe'
        $adb = Join-Path $cand 'platform-tools\adb.exe'
        if ((Test-Path -LiteralPath $emu) -and (Test-Path -LiteralPath $adb)) {
            return (Resolve-Path -LiteralPath $cand).Path
        }
    }

    Write-Err 'No se encontro un SDK de Android valido.'
    Write-Host 'Rutas probadas:' -ForegroundColor Red
    foreach ($cand in $candidates) { Write-Host "  - $cand" -ForegroundColor Red }
    Write-Host 'Define ANDROID_HOME o revisa sdk.dir en local.properties.' -ForegroundColor Yellow
    exit 1
}

$SdkRoot   = Resolve-SdkPath
$Emulator  = Join-Path $SdkRoot 'emulator\emulator.exe'
$Adb       = Join-Path $SdkRoot 'platform-tools\adb.exe'

# ---------------------------------------------------------------------------
# Helpers de adb / APK
# ---------------------------------------------------------------------------
function Get-AdbSerials {
    $out = & $Adb devices 2>&1
    $serials = @()
    foreach ($line in $out) {
        if ($line -match '^(\S+)\s+device\s*$') { $serials += $Matches[1] }
    }
    return $serials
}

function Get-EmulatorSerials {
    $all = @(Get-AdbSerials)
    $emu = @()
    foreach ($s in $all) { if ($s -like 'emulator-*') { $emu += $s } }
    return $emu
}

function Get-AvdNameOf {
    param([string]$Serial)
    try {
        $name = (& $Adb -s $Serial emu avd name 2>&1) | Select-Object -First 1
        if ($name) { return $name.Trim() }
    } catch { }
    return ''
}

function Get-ApkPath {
    if ([System.IO.Path]::IsPathRooted($Apk)) { return $Apk }
    return (Join-Path $RepoRoot $Apk)
}

function Invoke-InstallAndLaunch {
    param([string]$Serial)

    $apkPath = Get-ApkPath
    if (Test-Path -LiteralPath $apkPath) {
        Write-Step "Instalando APK: $apkPath"
        $installOut = & $Adb -s $Serial install -r $apkPath 2>&1
        $installOut | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
        if (($installOut -join "`n") -notmatch 'Success') {
            Write-Err 'La instalacion del APK fallo.'
            exit 1
        }
        Write-Ok 'APK instalado.'
    } else {
        Write-Warn "APK no encontrado, se omite la instalacion: $apkPath"
    }

    Write-Step "Lanzando $LaunchComp"
    $startOut = & $Adb -s $Serial shell am start -n $LaunchComp 2>&1
    $startOut | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
    if (($startOut -join "`n") -match 'Error|Exception') {
        Write-Err 'No se pudo lanzar la actividad.'
        exit 1
    }
    Write-Ok 'Aplicacion lanzada.'
}

function Wait-ForBoot {
    param([string]$Serial)

    $deadline = [DateTime]::UtcNow.AddSeconds($BootTimeout)
    Write-Step "Esperando sys.boot_completed=1 en $Serial (timeout ${BootTimeout}s)"
    while ([DateTime]::UtcNow -lt $deadline) {
        $val = (& $Adb -s $Serial shell getprop sys.boot_completed 2>&1) -join ''
        if ($val.Trim() -eq '1') {
            Write-Ok 'Boot completado.'
            return
        }
        Start-Sleep -Seconds 2
    }
    Stop-Script "Timeout esperando el boot del emulador ($Serial)."
}

# ---------------------------------------------------------------------------
# Acciones
# ---------------------------------------------------------------------------
function Invoke-Run {
    $runningSerial = $null
    foreach ($serial in @(Get-EmulatorSerials)) {
        if ((Get-AvdNameOf $serial) -eq $Avd) { $runningSerial = $serial; break }
    }

    if ($runningSerial) {
        Write-Warn "El AVD '$Avd' ya esta en marcha en $runningSerial. No se arranca otro."
        Wait-ForBoot $runningSerial
        Invoke-InstallAndLaunch $runningSerial
        return
    }

    $before = @(Get-EmulatorSerials)
    Write-Step "Arrancando AVD '$Avd' desde $Emulator"
    Start-Process -FilePath $Emulator -ArgumentList @('-avd', $Avd) | Out-Null
    Write-Ok 'Emulador lanzado en segundo plano.'

    Write-Step "Esperando a que adb detecte el dispositivo (timeout ${DeviceTimeout}s)"
    $deadline = [DateTime]::UtcNow.AddSeconds($DeviceTimeout)
    $serial = $null
    while ([DateTime]::UtcNow -lt $deadline) {
        foreach ($s in @(Get-EmulatorSerials)) {
            if ($before -notcontains $s) { $serial = $s; break }
        }
        if ($serial) { break }
        Start-Sleep -Seconds 2
    }
    if (-not $serial) { Stop-Script 'adb no detecto ningun emulador nuevo.' }
    Write-Ok "Dispositivo detectado: $serial"

    & $Adb -s $serial wait-for-device | Out-Null
    Wait-ForBoot $serial
    Invoke-InstallAndLaunch $serial
}

function Invoke-Kill {
    $emus = @(Get-EmulatorSerials)
    if ($emus.Count -eq 0) {
        Write-Warn 'No hay emuladores activos.'
        exit 0
    }
    foreach ($serial in $emus) {
        Write-Step "Apagando $serial"
        & $Adb -s $serial emu kill 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
    }
    Start-Sleep -Seconds 3
    $left = @(Get-EmulatorSerials)
    if ($left.Count -eq 0) {
        Write-Ok 'Todos los emuladores se apagaron.'
    } else {
        Write-Warn ("Siguen activos: " + ($left -join ', '))
    }
}

function Invoke-List {
    Write-Host 'AVDs disponibles:' -ForegroundColor Cyan
    $avds = & $Emulator -list-avds 2>&1
    $any = $false
    foreach ($a in $avds) {
        if ($a -and ($a -notmatch '^(INFO|WARNING|ERROR|emulator:)')) { Write-Host "  - $a"; $any = $true }
    }
    if (-not $any) { Write-Warn '  (ninguno)' }

    Write-Host 'Dispositivos adb:' -ForegroundColor Cyan
    $devices = & $Adb devices 2>&1
    $devices | ForEach-Object { Write-Host "  $_" }

    foreach ($serial in @(Get-EmulatorSerials)) {
        $name = Get-AvdNameOf $serial
        Write-Host "  $serial -> AVD: $name" -ForegroundColor Green
    }
}

function Invoke-Install {
    $serial = $null
    foreach ($s in @(Get-EmulatorSerials)) {
        if ((Get-AvdNameOf $s) -eq $Avd) { $serial = $s; break }
    }
    if (-not $serial) {
        $emus = @(Get-EmulatorSerials)
        if ($emus.Count -gt 0) {
            $serial = $emus[0]
            Write-Warn "No hay un emulador del AVD '$Avd'; se usa $serial."
        }
    }
    if (-not $serial) {
        $others = @(Get-AdbSerials)
        if ($others.Count -gt 0) {
            $serial = $others[0]
            Write-Warn "No hay emulador; se usa el dispositivo $serial."
        }
    }
    if (-not $serial) { Stop-Script 'No hay ningun dispositivo encendido. Usa -Action run.' }

    Invoke-InstallAndLaunch $serial
}

function Invoke-Logs {
    $serial = $null
    foreach ($s in @(Get-EmulatorSerials)) {
        if ((Get-AvdNameOf $s) -eq $Avd) { $serial = $s; break }
    }
    if (-not $serial) {
        $emus = @(Get-EmulatorSerials)
        if ($emus.Count -gt 0) { $serial = $emus[0] }
    }
    if (-not $serial) {
        $others = @(Get-AdbSerials)
        if ($others.Count -gt 0) { $serial = $others[0] }
    }
    if (-not $serial) { Stop-Script 'No hay ningun dispositivo encendido. Usa -Action run.' }

    $pidText = (& $Adb -s $serial shell pidof $PackageName 2>&1) -join ''
    $appPid = $pidText.Trim()

    if (-not $appPid) {
        Write-Warn "La app $PackageName no esta corriendo. Se muestra logcat general."
        Write-Host 'Ctrl+C para salir.' -ForegroundColor Yellow
        & $Adb -s $serial logcat
        return
    }

    Write-Ok "PID de $PackageName : $appPid"
    Write-Host 'Ctrl+C para salir.' -ForegroundColor Yellow
    & $Adb -s $serial logcat --pid=$appPid
}

switch ($Action) {
    'run'     {
        Write-Host "SDK: $SdkRoot" -ForegroundColor DarkGray
        Invoke-Run
    }
    'kill'    { Invoke-Kill }
    'list'    { Invoke-List }
    'install' {
        Write-Host "SDK: $SdkRoot" -ForegroundColor DarkGray
        Invoke-Install
    }
    'logs'    { Invoke-Logs }
    default   { Stop-Script "Accion no soportada: $Action" }
}

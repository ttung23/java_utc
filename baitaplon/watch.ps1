# ========================================================================
# SMART LIBRARY MANAGEMENT SYSTEM (SLMS) - DEV WATCHER & AUTO RELOAD
# ========================================================================

$Host.UI.RawUI.WindowTitle = "SLMS Dev Server - Auto Reload Watcher"
Clear-Host

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "      SLMS DEV SERVER - CHE DO TU DONG BUILD & LIVE RELOAD            " -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host ""

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

$mavenWrapper = Join-Path $projectDir "mvnw.cmd"
if (!(Test-Path $mavenWrapper)) {
    throw "Khong tim thay Maven Wrapper: $mavenWrapper"
}

$javaHome = $env:JAVA_HOME
if (!$javaHome -or !(Test-Path (Join-Path $javaHome "bin\javac.exe"))) {
    $javaHome = $null
    $javaRoots = @(
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft")
    )
    foreach ($root in $javaRoots) {
        $candidate = Get-ChildItem -Path $root -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName "bin\javac.exe") } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($candidate) {
            $javaHome = $candidate.FullName
            break
        }
    }
}
if (!$javaHome) {
    throw "Khong tim thay JDK 17+. Hay cai JDK hoac sua bien JAVA_HOME."
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"
$java = Join-Path $javaHome "bin\java.exe"
$classpathFile = Join-Path $projectDir "target\dependency-classpath.txt"

$global:serverProcess = $null

function Start-Server {
    Write-Host "`n>>> [SERVER] Dang khoi dong Java Web Server..." -ForegroundColor Green
    $dependencies = (Get-Content $classpathFile -Raw).Trim()
    $classpath = "$(Join-Path $projectDir 'target\classes');$dependencies"
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $java
    $startInfo.Arguments = "-cp `"$classpath`" com.slms.App --port=8081"
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $false
    $startInfo.RedirectStandardError = $false
    $global:serverProcess = [System.Diagnostics.Process]::Start($startInfo)
    Write-Host ">>> [SERVER] Server da chay tai: http://localhost:8081 (PID: $($global:serverProcess.Id))" -ForegroundColor Cyan
    Write-Host ">>> [WATCHER] Dang theo doi thay doi trong src/ (Sua code la tu dong build lai)..." -ForegroundColor Yellow
}

function Stop-Server {
    if ($global:serverProcess -and !$global:serverProcess.HasExited) {
        try {
            Write-Host ">>> [WATCHER] Dang dung server cu (PID: $($global:serverProcess.Id))..." -ForegroundColor DarkYellow
            $global:serverProcess.Kill()
            $global:serverProcess.WaitForExit(2000)
        } catch {}
    }
}

function Build-Java {
    Write-Host "`n>>> [BUILD] Dang bien dich Java va nap classpath..." -ForegroundColor Magenta
    & $mavenWrapper -q compile dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
    if ($LASTEXITCODE -eq 0 -and (Test-Path $classpathFile)) {
        Write-Host ">>> [BUILD] Bien dich thanh cong 100%!" -ForegroundColor Green
        return $true
    } else {
        Write-Host ">>> [BUILD LOI] Bien dich that bai! Vui long kiem tra lai code vua sua." -ForegroundColor Red
        return $false
    }
}

# Build and start the server once before watching for changes.
if (Build-Java) {
    Start-Server
}

# Cấu hình FileSystemWatcher để theo dõi thay đổi
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = Join-Path $projectDir "src"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true
$watcher.NotifyFilter = [System.IO.NotifyFilters]'FileName, LastWrite, Size'

$lastChangeTime = [DateTime]::MinValue

try {
    while ($true) {
        $change = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, 500)
        if ($change.TimedOut) {
            Start-Sleep -Milliseconds 200
            continue
        }

        $now = [DateTime]::Now
        if (($now - $lastChangeTime).TotalMilliseconds -lt 1200) {
            continue # Debounce để tránh trigger nhiều lần cho 1 file save
        }
        $lastChangeTime = $now

        $ext = [System.IO.Path]::GetExtension($change.Name).ToLower()
        Write-Host "`n[CHINHSUA] File thay doi: $($change.Name)" -ForegroundColor Yellow

        if ($ext -eq ".java") {
            # Sửa file Java -> Rebuild và Restart Server
            Stop-Server
            $ok = Build-Java
            if ($ok) {
                Start-Server
            } else {
                Write-Host ">>> Server tam dung cho den khi ban sua xong loi code Java." -ForegroundColor Red
            }
        } elseif ($ext -in @(".html", ".css", ".js")) {
            # Sửa Frontend -> Trình duyệt sẽ tự động cập nhật ngay lập tức khi bạn xem trang
            Write-Host ">>> [LIVE RELOAD] Giao dien web da cap nhat! Trinh duyet dang tu dong refresh..." -ForegroundColor Cyan
            Stop-Server
            Start-Server
        }
    }
} finally {
    Stop-Server
    $watcher.Dispose()
}


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

# 1. Cấu hình Java 22
$javac = "javac.exe"
$java = "java.exe"
if (Test-Path "C:\Program Files\Java\jdk-22\bin") {
    $javac = "C:\Program Files\Java\jdk-22\bin\javac.exe"
    $java = "C:\Program Files\Java\jdk-22\bin\java.exe"
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
    $env:PATH = "C:\Program Files\Java\jdk-22\bin;$env:PATH"
}

# 2. Đảm bảo thư mục bin và lib
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Force -Path "bin" | Out-Null }
if (!(Test-Path "lib")) { New-Item -ItemType Directory -Force -Path "lib" | Out-Null }

$global:serverProcess = $null

function Start-Server {
    Write-Host "`n>>> [SERVER] Dang khoi dong Java Web Server..." -ForegroundColor Green
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $java
    $startInfo.Arguments = "-cp bin;lib/gson.jar;lib/ojdbc11.jar com.slms.App"
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $false
    $startInfo.RedirectStandardError = $false
    $global:serverProcess = [System.Diagnostics.Process]::Start($startInfo)
    Write-Host ">>> [SERVER] Server da chay tai: http://localhost:8080 (PID: $($global:serverProcess.Id))" -ForegroundColor Cyan
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
    Write-Host "`n>>> [BUILD] Phat hien code Java thay doi! Dang bien dich lai..." -ForegroundColor Magenta
    $sources = (Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java").FullName
    $cp = "lib/gson.jar;lib/ojdbc11.jar;bin"
    
    $proc = Start-Process -FilePath $javac -ArgumentList @("-encoding", "UTF-8", "-cp", $cp, "-d", "bin") + $sources -NoNewWindow -Wait -PassThru
    if ($proc.ExitCode -eq 0) {
        Write-Host ">>> [BUILD] Bien dich thanh cong 100%!" -ForegroundColor Green
        return $true
    } else {
        Write-Host ">>> [BUILD LOI] Bien dich that bai! Vui long kiem tra lai code vua sua." -ForegroundColor Red
        return $false
    }
}

# Chạy server lần đầu
Start-Server

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


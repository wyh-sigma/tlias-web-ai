#requires -Version 7.0
<#
.SYNOPSIS
    一键压测：起压测实例（loadtest profile，假模型）→ 跑 k6 → 采样服务端指标 → 收尾。

.DESCRIPTION
    mock 模式不花钱、不受供应商限流，适合找容量；
    real 模式只开 1~3 VU，测真实端到端延迟与成本；
    fault 模式验证 429 / 超时 / 断流的兜底。

.EXAMPLE
    pwsh loadtest/run-loadtest.ps1 -Mode mock -Stage standard
    pwsh loadtest/run-loadtest.ps1 -Mode real -BaseUrl http://localhost:8080 -Duration 1m
    pwsh loadtest/run-loadtest.ps1 -Mode fault
#>
param(
    [ValidateSet('mock', 'real', 'fault')]
    [string]$Mode = 'mock',
    [ValidateSet('standard', 'spike', 'soak')]
    [string]$Stage = 'standard',
    [string]$BaseUrl = '',
    [string]$Duration = '',
    [ValidateSet('429', 'timeout', 'abort')]
    [string]$Fault = '429',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$k6 = Join-Path $root 'tools\k6\k6-v2.4.0-windows-amd64\k6.exe'
if (-not (Test-Path $k6)) {
    throw "找不到 k6：$k6。请先下载 k6 Windows 包到 tools\k6 下。"
}

if ($Mode -eq 'real' -and -not $BaseUrl) { $BaseUrl = 'http://localhost:8080' }
if (-not $BaseUrl) { $BaseUrl = 'http://localhost:8081' }

Push-Location $root
try {
    if (-not $SkipBuild) {
        Write-Host "[1/3] 构建镜像并启动依赖 ..." -ForegroundColor Cyan
        docker compose build app | Out-Host
        docker compose up -d mysql redis | Out-Host
    }

    if ($Mode -ne 'real') {
        Write-Host "[2/3] 启动压测实例（loadtest profile, 8081）..." -ForegroundColor Cyan
        docker compose --profile loadtest up -d app-loadtest | Out-Host
        $deadline = (Get-Date).AddMinutes(3)
        do {
            Start-Sleep -Seconds 3
            $healthy = (docker inspect -f '{{.State.Health.Status}}' tlias-app-loadtest 2>$null)
        } while ($healthy -ne 'healthy' -and (Get-Date) -lt $deadline)
        if ($healthy -ne 'healthy') { throw "压测实例未就绪（健康状态：$healthy），docker logs tlias-app-loadtest 查看原因" }
        Write-Host "压测实例已就绪：$BaseUrl" -ForegroundColor Green
    }

    Write-Host "[3/3] 开始压测 ..." -ForegroundColor Cyan
    $script = if ($Mode -eq 'real' -or $Mode -eq 'fault') {
        Join-Path $root 'loadtest\k6-ai.js'
    } else {
        Join-Path $root 'loadtest\k6.js'
    }
    $args = @('run', '-e', "BASE_URL=$BaseUrl")
    $args += @('-e', "MODE=$Mode", '-e', "STAGE=$Stage", '-e', "FAULT=$Fault")
    if ($Duration) { $args += @('-e', "DURATION=$Duration") }
    $args += $script

    & $k6 @args
    $exit = $LASTEXITCODE

    Write-Host "本次结果已写入：$root\loadtest\server-metrics.csv（如已启动采样脚本）" -ForegroundColor Green
    exit $exit
}
finally {
    Pop-Location
}

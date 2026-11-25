# 压测期间的服务端指标采集
# 一边跑 k6，一边执行本脚本，即可看到服务器在压力下的真实状态：
#   - /monitor/overview：总请求量、接口耗时排行
#   - /monitor/ai      ：AI 命中率、首字延迟、Token、限流
#   - docker stats     ：应用容器 CPU/内存
#
# 用法（先启动服务与 Docker Desktop）：
#   powershell -ExecutionPolicy Bypass -File .\loadtest\collect-server-metrics.ps1 -DurationSeconds 90
#
# 说明：k6 跑 60~90 秒时，建议并发执行本脚本，结束后查看同目录的 server-metrics.csv。

param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Username = 'admin',
    [string]$Password = '123456',
    [int]$DurationSeconds = 90,
    [int]$IntervalSeconds = 3,
    [string]$OutFile = (Join-Path $PSScriptRoot 'server-metrics.csv')
)


function Login-Token {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $body -ContentType 'application/json' -TimeoutSec 10
    if (-not $res -or $res.code -ne 1 -or -not $res.data.token) {
        throw "登录失败，请确认服务已启动：$BaseUrl"
    }
    return $res.data.token
}

$token = Login-Token
$headers = @{ Authorization = "Bearer $token" }
Write-Host "开始采集服务端指标：$BaseUrl，持续 $DurationSeconds 秒，间隔 $IntervalSeconds 秒" -ForegroundColor Cyan
Write-Host "同时可另开终端运行 k6 压测。" -ForegroundColor Cyan

$rows = @()
$end = (Get-Date).AddSeconds($DurationSeconds)
$maxTotalRequests = 0
$maxAppCpu = 0
$maxAiResponse = 0

while ((Get-Date) -lt $end) {
    $ts = (Get-Date).ToString('HH:mm:ss')

    $ov = $null
    $ai = $null
    try { $ov = Invoke-RestMethod -Uri "$BaseUrl/monitor/overview" -Headers $headers -TimeoutSec 5 } catch {}
    try { $ai = Invoke-RestMethod -Uri "$BaseUrl/monitor/ai" -Headers $headers -TimeoutSec 5 } catch {}

    # 容器资源：只取应用容器
    $appCpu = ''
    $appMem = ''
    $stats = & docker stats --no-stream --format '{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}' 2>$null
    foreach ($line in $stats) {
        if ($line -like 'tlias-app*') {
            $parts = $line -split '\|'
            if ($parts.Count -ge 3) {
                $appCpu = $parts[1]
                $appMem = $parts[2]
            }
        }
    }

    $totalRequests = if ($ov -and $ov.data) { $ov.data.totalRequests } else { $null }
    $aiRequests = if ($ai -and $ai.data) { $ai.data.aiRequests } else { $null }
    $kbHitRate = if ($ai -and $ai.data) { $ai.data.kbHitRate } else { $null }
    $avgResponseMs = if ($ai -and $ai.data) { $ai.data.avgResponseMs } else { $null }
    $p95ResponseMs = if ($ai -and $ai.data) { $ai.data.p95ResponseMs } else { $null }
    $p99ResponseMs = if ($ai -and $ai.data) { $ai.data.p99ResponseMs } else { $null }
    $avgFirstTokenMs = if ($ai -and $ai.data) { $ai.data.avgFirstTokenMs } else { $null }
    $p95FirstTokenMs = if ($ai -and $ai.data) { $ai.data.p95FirstTokenMs } else { $null }
    $totalTokens = if ($ai -and $ai.data) { $ai.data.totalTokens } else { $null }
    $rateLimited = if ($ai -and $ai.data) { $ai.data.rateLimitedCount } else { $null }
    $fallbackCount = if ($ai -and $ai.data) { $ai.data.fallbackCount } else { $null }
    $inflight = if ($ai -and $ai.data) { $ai.data.inflight } else { $null }

    if ($totalRequests -gt $maxTotalRequests) { $maxTotalRequests = $totalRequests }
    if ($avgResponseMs -gt $maxAiResponse) { $maxAiResponse = $avgResponseMs }
    $cpuNum = 0.0
    if ($appCpu -match '([\d.]+)%') { $cpuNum = [double]$Matches[1] }
    if ($cpuNum -gt $maxAppCpu) { $maxAppCpu = $cpuNum }

    $rows += [pscustomobject]@{
        time             = $ts
        totalRequests    = $totalRequests
        appCpu           = $appCpu
        appMem           = $appMem
        aiRequests       = $aiRequests
        inflight         = $inflight
        kbHitRate        = $kbHitRate
        avgResponseMs    = $avgResponseMs
        p95ResponseMs    = $p95ResponseMs
        p99ResponseMs    = $p99ResponseMs
        avgFirstTokenMs  = $avgFirstTokenMs
        p95FirstTokenMs  = $p95FirstTokenMs
        totalTokens      = $totalTokens
        rateLimitedCount = $rateLimited
        fallbackCount    = $fallbackCount
    }

    Write-Host ("[{0}] 总请求={1}  app CPU={2}  内存={3}  AI请求={4}  并发={5}  命中率={6}%  p95={7}ms  首字p95={8}ms" -f `
        $ts, $totalRequests, $appCpu, $appMem, $aiRequests, $inflight, $kbHitRate, $p95ResponseMs, $p95FirstTokenMs)

    Start-Sleep -Seconds $IntervalSeconds
}

$rows | Export-Csv -Path $OutFile -NoTypeInformation -Encoding UTF8
Write-Host ""
Write-Host "采集完成：$OutFile" -ForegroundColor Green
Write-Host ("峰值：总请求={0}，应用 CPU={1}%，AI 平均响应={2}ms" -f $maxTotalRequests, $maxAppCpu, $maxAiResponse) -ForegroundColor Green

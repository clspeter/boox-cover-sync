[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Serial,
    [ValidateRange(1, 600)]
    [int]$DurationSeconds = 90,
    [ValidateRange(1, 30)]
    [int]$IntervalSeconds = 2
)

$ErrorActionPreference = 'Stop'
$packageName = 'tw.mustp.booxcoversync'

function Read-AdbText {
    param([string[]]$CommandArguments)
    $result = & adb -s $Serial @CommandArguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw 'ADB state query failed. Check the USB connection and device authorization.'
    }
    return $result -join "`n"
}

function Read-LastExit {
    $exitInfo = Read-AdbText @('shell', 'dumpsys', 'activity', 'exit-info', $packageName)
    # Restrict every field to a timestamp, number, or boolean. Never serialize
    # raw exit descriptions, process traces, book paths, or other app data.
    $timestampMatch = [regex]::Match($exitInfo, 'timestamp=([0-9 :.-]+)')
    $reasonMatch = [regex]::Match($exitInfo, 'reason=(\d+)')
    $descriptionMatch = [regex]::Match($exitInfo, 'description=([^\r\n]*)')
    $callerMatch = [regex]::Match($descriptionMatch.Groups[1].Value, 'from pid (\d+)')
    return [ordered]@{
        deviceTimestamp = $(if ($timestampMatch.Success) { $timestampMatch.Groups[1].Value.Trim() } else { $null })
        reasonCode = $(if ($reasonMatch.Success) { [int]$reasonMatch.Groups[1].Value } else { $null })
        callerPid = $(if ($callerMatch.Success) { [int]$callerMatch.Groups[1].Value } else { $null })
        packageInstall = $(if ($descriptionMatch.Success) { $descriptionMatch.Groups[1].Value.Contains('installPackageLI') } else { $null })
    }
}

$clock = [Diagnostics.Stopwatch]::StartNew()
$previousState = $null
$sampleCount = 0
Write-Host 'Read-only observation. Reproduce sleep/wake or return-to-library; no app restart or UI inspection is performed.'
while ($clock.Elapsed.TotalSeconds -lt $DurationSeconds) {
    $power = Read-AdbText @('shell', 'dumpsys', 'power')
    $packageState = Read-AdbText @('shell', 'dumpsys', 'package', $packageName)
    $accessibility = Read-AdbText @('shell', 'dumpsys', 'accessibility')
    $setting = Read-AdbText @('shell', 'settings', 'get', 'secure', 'enabled_accessibility_services')
    $screenMatch = [regex]::Match($power, 'mWakefulness=(\w+)')
    $stoppedMatch = [regex]::Match($packageState, 'User 0:[^\r\n]*\bstopped=(true|false)')
    $boundMatch = [regex]::Match($accessibility, '(?s)Bound services:(.*?)Enabled services:')
    $state = [ordered]@{
        screen = $(if ($screenMatch.Success) { $screenMatch.Groups[1].Value } else { 'unknown' })
        packageStopped = $(if ($stoppedMatch.Success) { $stoppedMatch.Groups[1].Value -eq 'true' } else { $null })
        accessibilityEnabled = $setting.Contains("$packageName/")
        serviceBound = $(if ($boundMatch.Success) { $boundMatch.Groups[1].Value.Contains('BOOX Cover Sync') } else { $null })
        serviceListedCrashed = [bool]($accessibility -match 'Crashed services:[^\r\n]*tw\.mustp\.booxcoversync/')
    }
    $serializedState = $state | ConvertTo-Json -Compress
    if ($serializedState -ne $previousState) {
        [ordered]@{
            observedAt = [DateTimeOffset]::Now.ToString('o')
            elapsedSeconds = [math]::Round($clock.Elapsed.TotalSeconds, 1)
            state = $state
            lastExit = Read-LastExit
        } | ConvertTo-Json -Depth 4 -Compress
        $previousState = $serializedState
    }
    $sampleCount += 1
    $remaining = $DurationSeconds - $clock.Elapsed.TotalSeconds
    if ($remaining -gt 0) {
        Start-Sleep -Milliseconds ([int][math]::Min($IntervalSeconds * 1000, $remaining * 1000))
    }
}
Write-Host "Observation finished: $sampleCount samples. Exit reason 10 alone does not identify a human action."

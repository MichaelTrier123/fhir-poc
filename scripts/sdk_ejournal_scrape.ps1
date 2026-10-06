<#
.SYNOPSIS
    Scrape eJournal data from demo.sdkdev.dk using an existing
    forloebsoversigt JSON file.

.DESCRIPTION
    The script:

      1. Reads the overview JSON.
      2. Extracts CPR from Overview.PersonNummer.
      3. Normalizes CPR by removing "-".
      4. Creates:
           ejournal-<cpr>/

      5. Copies the overview to:
           forloebsoversigt-<cpr>.json

      6. Fetches:
           caveoplysninger-<cpr>.json

      7. Traverses Overview.Forloeb and fetches, where Antal* > 0:
           diagnoser-<uuid>.json
           notater-<uuid>.json
           procedurer-<uuid>.json
           kontaktperioder-<uuid>.json
           epikriser-<uuid>.json

    Session/authentication values are read from:

        EJOURNAL_COOKIE
        EJOURNAL_XSRF
        EJOURNAL_CONVERSATION_UUID

.EXAMPLE
    .\scrape_sdk_data.ps1 `
        -OverviewPath '.\forloebsoversigt.json'

.EXAMPLE
    .\scrape_sdk_data.ps1 `
        -OverviewPath '.\forloebsoversigt.json' `
        -ForceRefresh
#>

[CmdletBinding()]
param (
    [Parameter(Mandatory = $true)]
    [string]$OverviewPath,

    [string]$OutputDirectory,

    [switch]$ForceRefresh
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'


# ============================================================================
# Configuration
# ============================================================================

$BaseUrl = 'https://demo.sdkdev.dk/app/ejournalportalborger/api/ejournal'

$Referer = 'https://demo.sdkdev.dk/borger/min-side/min-sundhedsjournal/journal-fra-sygehus/'

$Cookie = $env:EJOURNAL_COOKIE
$XsrfToken = $env:EJOURNAL_XSRF
$ConversationUuid = $env:EJOURNAL_CONVERSATION_UUID


# ============================================================================
# Functions
# ============================================================================

function Assert-Configuration {

    if ([string]::IsNullOrWhiteSpace($Cookie)) {
        throw 'EJOURNAL_COOKIE environment variable is not set.'
    }

    if ([string]::IsNullOrWhiteSpace($XsrfToken)) {
        throw 'EJOURNAL_XSRF environment variable is not set.'
    }

    if ([string]::IsNullOrWhiteSpace($ConversationUuid)) {
        throw 'EJOURNAL_CONVERSATION_UUID environment variable is not set.'
    }

    if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
        throw 'curl.exe could not be found in PATH.'
    }

    if (-not (Test-Path -LiteralPath $OverviewPath -PathType Leaf)) {
        throw "Overview file not found: $OverviewPath"
    }
}


function Test-JsonFile {
    param (
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    try {
        $Content = Get-Content `
            -LiteralPath $Path `
            -Raw `
            -Encoding UTF8

        $null = $Content | ConvertFrom-Json

        return $true
    }
    catch {
        return $false
    }
}


function Get-NormalizedCprFromOverview {
    param (
        [Parameter(Mandatory = $true)]
        [object]$Overview
    )

    $PersonNummer = [string]$Overview.PersonNummer

    if ([string]::IsNullOrWhiteSpace($PersonNummer)) {
        throw 'The overview JSON does not contain PersonNummer.'
    }

    $Normalized = $PersonNummer -replace '-', ''

    if ($Normalized -notmatch '^\d{10}$') {
        throw "Invalid PersonNummer in overview: $PersonNummer"
    }

    return $Normalized
}


function Get-KeyJson {
    param (
        [Parameter(Mandatory = $true)]
        [object]$IdNoegle
    )

    $KeyObject = [ordered]@{
        Database           = $IdNoegle.Database
        Noegle             = $IdNoegle.Noegle
        VaerdispringNoegle = $IdNoegle.VaerdispringNoegle
    }

    return ($KeyObject | ConvertTo-Json -Compress -Depth 10)
}


function Get-KeyedUrl {
    param (
        [Parameter(Mandatory = $true)]
        [string]$Endpoint,

        [Parameter(Mandatory = $true)]
        [object]$IdNoegle
    )

    $KeyJson = Get-KeyJson -IdNoegle $IdNoegle

    #
    # Encode the JSON here before passing it to curl.exe.
    # This avoids PowerShell/native command quote stripping.
    #

    $EncodedKey = [System.Uri]::EscapeDataString($KeyJson)

    return "$BaseUrl/$Endpoint`?noegle=$EncodedKey"
}


function Invoke-EJournalJsonGet {
    param (
        [Parameter(Mandatory = $true)]
        [string]$Url,

        [Parameter(Mandatory = $true)]
        [string]$OutputPath,

        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    if (
        (-not $ForceRefresh) -and
        (Test-Path -LiteralPath $OutputPath -PathType Leaf)
    ) {
        Write-Host "SKIP $Description" -ForegroundColor DarkGray
        Write-Host "  Already exists: $OutputPath"

        return [pscustomobject]@{
            Status  = 'Skipped'
            Success = $true
        }
    }

    $TempPath = "$OutputPath.tmp"

    if (Test-Path -LiteralPath $TempPath) {
        Remove-Item `
            -LiteralPath $TempPath `
            -Force `
            -ErrorAction SilentlyContinue
    }

    Write-Host ''
    Write-Host "GET $Description" -ForegroundColor Cyan
    Write-Host "  Output: $OutputPath"

    $CurlArgs = @(
        '--silent'
        '--show-error'
        '--location'

        $Url

        '-H'
        'accept: application/json, text/plain, */*'

        '-H'
        'accept-language: da-DK,da;q=0.9,en-US;q=0.8,en;q=0.7'

        '-H'
        "conversation-uuid: $ConversationUuid"

        '-H'
        'dnt: 1'

        '-H'
        'page-app-id: 717'

        '-H'
        "referer: $Referer"

        '-H'
        "x-xsrf-token: $XsrfToken"

        '-b'
        $Cookie

        '-o'
        $TempPath

        '-w'
        '%{http_code}'
    )

    $StatusCode = (& curl.exe @CurlArgs).Trim()

    Write-Host "  HTTP:   $StatusCode"

    if ($StatusCode -eq '401' -or $StatusCode -eq '403') {

        $ServerResponse = $null

        if (Test-Path -LiteralPath $TempPath) {
            $ServerResponse = Get-Content `
                -LiteralPath $TempPath `
                -Raw `
                -ErrorAction SilentlyContinue
        }

        Remove-Item `
            -LiteralPath $TempPath `
            -Force `
            -ErrorAction SilentlyContinue

        if (-not [string]::IsNullOrWhiteSpace($ServerResponse)) {
            Write-Host ''
            Write-Host $ServerResponse -ForegroundColor Yellow
        }

        throw @"
The eJournal session was rejected with HTTP $StatusCode.

Refresh:

  EJOURNAL_COOKIE
  EJOURNAL_XSRF
  EJOURNAL_CONVERSATION_UUID
"@
    }

    if ($StatusCode -notmatch '^2\d\d$') {

        Write-Warning "$Description failed with HTTP $StatusCode"

        if (Test-Path -LiteralPath $TempPath) {

            $ServerResponse = Get-Content `
                -LiteralPath $TempPath `
                -Raw `
                -ErrorAction SilentlyContinue

            if (-not [string]::IsNullOrWhiteSpace($ServerResponse)) {
                Write-Host ''
                Write-Host 'Server response:' -ForegroundColor Yellow
                Write-Host $ServerResponse
            }

            Remove-Item `
                -LiteralPath $TempPath `
                -Force `
                -ErrorAction SilentlyContinue
        }

        return [pscustomobject]@{
            Status  = 'Failed'
            Success = $false
        }
    }

    if (-not (Test-Path -LiteralPath $TempPath -PathType Leaf)) {

        Write-Warning "$Description returned HTTP $StatusCode but created no response file."

        return [pscustomobject]@{
            Status  = 'Failed'
            Success = $false
        }
    }

    if (-not (Test-JsonFile -Path $TempPath)) {

        Write-Warning "$Description did not return valid JSON."

        $Content = Get-Content `
            -LiteralPath $TempPath `
            -Raw `
            -ErrorAction SilentlyContinue

        if (-not [string]::IsNullOrWhiteSpace($Content)) {
            Write-Host ''
            Write-Host 'Response:' -ForegroundColor Yellow
            Write-Host $Content
        }

        Remove-Item `
            -LiteralPath $TempPath `
            -Force `
            -ErrorAction SilentlyContinue

        return [pscustomobject]@{
            Status  = 'Failed'
            Success = $false
        }
    }

    Move-Item `
        -LiteralPath $TempPath `
        -Destination $OutputPath `
        -Force

    Write-Host '  Saved.' -ForegroundColor Green

    return [pscustomobject]@{
        Status  = 'Downloaded'
        Success = $true
    }
}


function Invoke-KeyedResource {
    param (
        [Parameter(Mandatory = $true)]
        [string]$Endpoint,

        [Parameter(Mandatory = $true)]
        [string]$FilePrefix,

        [Parameter(Mandatory = $true)]
        [object]$IdNoegle,

        [Parameter(Mandatory = $true)]
        [string]$OutputDirectory
    )

    $Uuid = [string]$IdNoegle.Noegle

    if ([string]::IsNullOrWhiteSpace($Uuid)) {
        throw 'IdNoegle.Noegle was empty.'
    }

    try {
        $null = [guid]::Parse($Uuid)
    }
    catch {
        throw "Invalid UUID in IdNoegle.Noegle: $Uuid"
    }

    $Url = Get-KeyedUrl `
        -Endpoint $Endpoint `
        -IdNoegle $IdNoegle

    $OutputPath = Join-Path `
        $OutputDirectory `
        "$FilePrefix-$Uuid.json"

    return Invoke-EJournalJsonGet `
        -Url $Url `
        -OutputPath $OutputPath `
        -Description "$Endpoint / $Uuid"
}


function Add-ResultToSummary {
    param (
        [Parameter(Mandatory = $true)]
        [object]$Result
    )

    switch ($Result.Status) {

        'Downloaded' {
            $script:DownloadedCount++
        }

        'Skipped' {
            $script:SkippedCount++
        }

        'Failed' {
            $script:FailedCount++
        }
    }
}


# ============================================================================
# Validate configuration
# ============================================================================

Assert-Configuration


# ============================================================================
# Read overview
# ============================================================================

if (-not (Test-JsonFile -Path $OverviewPath)) {
    throw "Overview file is not valid JSON: $OverviewPath"
}

$OverviewRaw = Get-Content `
    -LiteralPath $OverviewPath `
    -Raw `
    -Encoding UTF8

$Overview = $OverviewRaw | ConvertFrom-Json


# ============================================================================
# Derive CPR from overview
# ============================================================================

$OriginalCpr = [string]$Overview.PersonNummer
$NormalizedCpr = Get-NormalizedCprFromOverview -Overview $Overview


# ============================================================================
# Output directory
# ============================================================================

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = "ejournal-$NormalizedCpr"
}

$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)

New-Item `
    -ItemType Directory `
    -Path $OutputDirectory `
    -Force |
    Out-Null


# ============================================================================
# Header
# ============================================================================

Write-Host ''
Write-Host '============================================================'
Write-Host 'eJournal SDK scraper' -ForegroundColor Green
Write-Host '============================================================'
Write-Host "CPR from overview: $OriginalCpr"
Write-Host "Normalized CPR:    $NormalizedCpr"
Write-Host "Input:             $OverviewPath"
Write-Host "Output:            $OutputDirectory"
Write-Host "Force refresh:     $ForceRefresh"
Write-Host ''


# ============================================================================
# Save overview
# ============================================================================

$OverviewOutputPath = Join-Path `
    $OutputDirectory `
    "forloebsoversigt-$NormalizedCpr.json"

Copy-Item `
    -LiteralPath $OverviewPath `
    -Destination $OverviewOutputPath `
    -Force

Write-Host "Overview saved:"
Write-Host "  $OverviewOutputPath"


# ============================================================================
# Counters
# ============================================================================

$script:DownloadedCount = 0
$script:SkippedCount = 0
$script:FailedCount = 0
$script:NoDataCount = 0


# ============================================================================
# Fetch CAVE oplysninger
# ============================================================================

$CaveOutputPath = Join-Path `
    $OutputDirectory `
    "caveoplysninger-$NormalizedCpr.json"

$CaveResult = Invoke-EJournalJsonGet `
    -Url "$BaseUrl/caveoplysninger" `
    -OutputPath $CaveOutputPath `
    -Description 'caveoplysninger'

Add-ResultToSummary -Result $CaveResult


# ============================================================================
# Derived resources
# ============================================================================

$ResourceDefinitions = @(

    [pscustomobject]@{
        Counter    = 'AntalDiagnoser'
        Endpoint   = 'diagnoser'
        FilePrefix = 'diagnoser'
    }

    [pscustomobject]@{
        Counter    = 'AntalNotater'
        Endpoint   = 'notater'
        FilePrefix = 'notater'
    }

    [pscustomobject]@{
        Counter    = 'AntalProcedurer'
        Endpoint   = 'procedurer'
        FilePrefix = 'procedurer'
    }

    [pscustomobject]@{
        Counter    = 'AntalKontaktperioder'
        Endpoint   = 'kontaktperioder'
        FilePrefix = 'kontaktperioder'
    }

    [pscustomobject]@{
        Counter    = 'AntalEpikriser'
        Endpoint   = 'epikriser'
        FilePrefix = 'epikriser'
    }
)


# ============================================================================
# Traverse Forloeb
# ============================================================================

$ForloebList = @($Overview.Forloeb)
$TotalForloeb = $ForloebList.Count
$ForloebNumber = 0

Write-Host ''
Write-Host "Forloeb found: $TotalForloeb"


foreach ($Forloeb in $ForloebList) {

    $ForloebNumber++

    Write-Host ''
    Write-Host '============================================================'
    Write-Host "Forloeb $ForloebNumber / $TotalForloeb" -ForegroundColor Green
    Write-Host '============================================================'

    if ($null -eq $Forloeb.IdNoegle) {
        Write-Warning 'Forloeb has no IdNoegle. Skipping.'
        continue
    }

    $Uuid = [string]$Forloeb.IdNoegle.Noegle

    if ([string]::IsNullOrWhiteSpace($Uuid)) {
        Write-Warning 'Forloeb has no IdNoegle.Noegle. Skipping.'
        continue
    }

    Write-Host "Noegle:     $Uuid"
    Write-Host "Hospital:   $($Forloeb.SygehusNavn)"
    Write-Host "Department: $($Forloeb.AfdelingNavn)"
    Write-Host "DatoFra:    $($Forloeb.DatoFra)"

    foreach ($Definition in $ResourceDefinitions) {

        $CounterProperty = $Forloeb.PSObject.Properties[$Definition.Counter]

        if ($null -eq $CounterProperty) {
            $Count = 0
        }
        else {
            $Count = [int]$CounterProperty.Value
        }

        Write-Host "$($Definition.Counter): $Count"

        if ($Count -le 0) {
            $script:NoDataCount++
            continue
        }

        try {

            $Result = Invoke-KeyedResource `
                -Endpoint $Definition.Endpoint `
                -FilePrefix $Definition.FilePrefix `
                -IdNoegle $Forloeb.IdNoegle `
                -OutputDirectory $OutputDirectory

            Add-ResultToSummary -Result $Result
        }
        catch {

            if (
                $_.Exception.Message -like '*session was rejected*' -or
                $_.Exception.Message -like '*EJOURNAL_*'
            ) {
                throw
            }

            $script:FailedCount++

            Write-Warning "$($Definition.Endpoint) failed for $Uuid"
            Write-Warning $_.Exception.Message
        }
    }
}


# ============================================================================
# Summary
# ============================================================================

Write-Host ''
Write-Host '============================================================'
Write-Host 'Finished' -ForegroundColor Green
Write-Host '============================================================'
Write-Host "CPR:                $NormalizedCpr"
Write-Host "Forloeb processed:  $TotalForloeb"
Write-Host "Downloaded:         $DownloadedCount"
Write-Host "Already existing:   $SkippedCount"
Write-Host "No data / skipped:  $NoDataCount"
Write-Host "Failed:             $FailedCount"
Write-Host ''
Write-Host 'Output directory:'
Write-Host "  $OutputDirectory"
Write-Host ''

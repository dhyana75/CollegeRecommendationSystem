$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$srcRoot = Join-Path $projectRoot 'src'
$mysqlJar = Join-Path $projectRoot 'lib\mysql-connector-j-26.7.0.jar'
$outDir = Join-Path $projectRoot 'out'
$sourceList = Join-Path $projectRoot 'sources.txt'

if (-not (Test-Path $srcRoot)) {
    Write-Error "Could not find the src folder: $srcRoot"
    exit 1
}
if (-not (Test-Path $mysqlJar)) {
    Write-Error "Could not find the MySQL Connector/J JAR: $mysqlJar"
    exit 1
}

if (Test-Path $outDir) {
    Remove-Item -Recurse -Force $outDir
}
New-Item -ItemType Directory -Path $outDir | Out-Null

$sourceFiles = Get-ChildItem -Path $srcRoot -Recurse -File -Filter '*.java' |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }

if (-not $sourceFiles) {
    Write-Error "No Java source files were found under $srcRoot"
    exit 1
}

$sourceFiles | Set-Content -Path $sourceList -Encoding ASCII

try {
    $argFile = '@' + $sourceList
    & javac -encoding UTF-8 -cp $mysqlJar -d $outDir $argFile
    $buildResult = $LASTEXITCODE
}
finally {
    if (Test-Path $sourceList) {
        Remove-Item -Force $sourceList
    }
}

if ($buildResult -ne 0) {
    exit $buildResult
}

exit 0

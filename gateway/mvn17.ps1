$previousJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = 'C:\Users\Asus\.jdks\ms-17.0.20.1'
Push-Location $PSScriptRoot
try {
    & 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd' @args
    $result = $LASTEXITCODE
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
}
exit $result

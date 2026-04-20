# Sapiens app 모듈: 컴파일 + Android Lint (푸시 전 실행 권장)
# - app/google-services.json 이 없을 때만 CI용 플레이스홀더를 잠시 복사합니다(로컬 실제 파일을 덮어쓰지 않음).
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$gs = Join-Path $repoRoot "app/google-services.json"
$needTemp = -not (Test-Path $gs)
if ($needTemp) {
    Copy-Item (Join-Path $repoRoot "tools/google-services.ci.json") $gs -Force
    Write-Host "[verify-app] google-services.json 없음 -> CI 플레이스홀더로 빌드합니다."
}

try {
    & (Join-Path $repoRoot "gradlew.bat") ":app:verifyApp" --no-daemon @args
    exit $LASTEXITCODE
}
finally {
    if ($needTemp -and (Test-Path $gs)) {
        Remove-Item $gs -Force -ErrorAction SilentlyContinue
        Write-Host "[verify-app] 임시 google-services.json 제거됨."
    }
}

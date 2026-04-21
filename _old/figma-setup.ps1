# Figma Variables 자동 등록 스크립트
# 실행 전: YOUR_TOKEN 부분을 본인 Personal Access Token으로 교체

# 토큰은 저장소에 커밋하지 마세요. 환경변수 예: $env:FIGMA_TOKEN
$TOKEN = $env:FIGMA_TOKEN
if (-not $TOKEN) { throw "FIGMA_TOKEN 환경변수를 설정한 뒤 다시 실행하세요." }
$FILE_KEY = "EaE3QXT7UV66RFZRXppOvi"
$BASE_URL = "https://api.figma.com/v1"

$headers = @{
    "X-Figma-Token" = $TOKEN
    "Content-Type" = "application/json"
}

Write-Host "Figma Variables 등록 시작..." -ForegroundColor Cyan

# 1. Variable Collection 생성 (Color)
$colorCollectionBody = @{
    variableCollections = @(
        @{
            action = "CREATE"
            id = "color-collection"
            name = "Sapiens Colors"
            initialModeId = "dark-mode"
        }
    )
    variableModes = @(
        @{
            action = "CREATE"
            id = "dark-mode"
            name = "Dark"
            variableCollectionId = "color-collection"
        },
        @{
            action = "CREATE"
            id = "light-mode"
            name = "Light"
            variableCollectionId = "color-collection"
        }
    )
    variables = @(
        @{ action = "CREATE"; id = "var-background"; name = "Background"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-card"; name = "Card"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-elevated"; name = "Elevated"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-textPrimary"; name = "TextPrimary"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-textSecondary"; name = "TextSecondary"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-hair"; name = "Hair"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-accent"; name = "Accent"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-marketUp"; name = "MarketUp"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-marketDown"; name = "MarketDown"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-marketFlat"; name = "MarketFlat"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-surfaceMuted"; name = "SurfaceMuted"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-success"; name = "Success"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-warning"; name = "Warning"; variableCollectionId = "color-collection"; resolvedType = "COLOR" },
        @{ action = "CREATE"; id = "var-error"; name = "Error"; variableCollectionId = "color-collection"; resolvedType = "COLOR" }
    )
    variableValues = @(
        # Dark mode values
        @{ variableId = "var-background"; modeId = "dark-mode"; value = @{ r = 0.082; g = 0.078; b = 0.098; a = 1 } },
        @{ variableId = "var-card"; modeId = "dark-mode"; value = @{ r = 0.106; g = 0.106; b = 0.118; a = 1 } },
        @{ variableId = "var-elevated"; modeId = "dark-mode"; value = @{ r = 0.149; g = 0.149; b = 0.149; a = 1 } },
        @{ variableId = "var-textPrimary"; modeId = "dark-mode"; value = @{ r = 0.984; g = 0.984; b = 0.984; a = 1 } },
        @{ variableId = "var-textSecondary"; modeId = "dark-mode"; value = @{ r = 0.529; g = 0.529; b = 0.529; a = 1 } },
        @{ variableId = "var-hair"; modeId = "dark-mode"; value = @{ r = 1; g = 1; b = 1; a = 0.149 } },
        @{ variableId = "var-accent"; modeId = "dark-mode"; value = @{ r = 0.961; g = 0.431; b = 0.059; a = 1 } },
        @{ variableId = "var-marketUp"; modeId = "dark-mode"; value = @{ r = 1; g = 0.231; b = 0.188; a = 1 } },
        @{ variableId = "var-marketDown"; modeId = "dark-mode"; value = @{ r = 0; g = 0.392; b = 1; a = 1 } },
        @{ variableId = "var-marketFlat"; modeId = "dark-mode"; value = @{ r = 0.529; g = 0.529; b = 0.529; a = 1 } },
        @{ variableId = "var-surfaceMuted"; modeId = "dark-mode"; value = @{ r = 0.157; g = 0.157; b = 0.165; a = 1 } },
        @{ variableId = "var-success"; modeId = "dark-mode"; value = @{ r = 0.114; g = 0.620; b = 0.459; a = 1 } },
        @{ variableId = "var-warning"; modeId = "dark-mode"; value = @{ r = 0.937; g = 0.624; b = 0.153; a = 1 } },
        @{ variableId = "var-error"; modeId = "dark-mode"; value = @{ r = 0.886; g = 0.294; b = 0.290; a = 1 } },
        # Light mode values
        @{ variableId = "var-background"; modeId = "light-mode"; value = @{ r = 0.980; g = 0.980; b = 0.973; a = 1 } },
        @{ variableId = "var-card"; modeId = "light-mode"; value = @{ r = 1; g = 1; b = 1; a = 1 } },
        @{ variableId = "var-elevated"; modeId = "light-mode"; value = @{ r = 0.949; g = 0.945; b = 0.933; a = 1 } },
        @{ variableId = "var-textPrimary"; modeId = "light-mode"; value = @{ r = 0.102; g = 0.102; b = 0.102; a = 1 } },
        @{ variableId = "var-textSecondary"; modeId = "light-mode"; value = @{ r = 0.420; g = 0.420; b = 0.420; a = 1 } },
        @{ variableId = "var-hair"; modeId = "light-mode"; value = @{ r = 0; g = 0; b = 0; a = 0.078 } },
        @{ variableId = "var-accent"; modeId = "light-mode"; value = @{ r = 0.961; g = 0.431; b = 0.059; a = 1 } },
        @{ variableId = "var-marketUp"; modeId = "light-mode"; value = @{ r = 0.878; g = 0.161; b = 0.122; a = 1 } },
        @{ variableId = "var-marketDown"; modeId = "light-mode"; value = @{ r = 0; g = 0.322; b = 0.800; a = 1 } },
        @{ variableId = "var-marketFlat"; modeId = "light-mode"; value = @{ r = 0.529; g = 0.529; b = 0.529; a = 1 } },
        @{ variableId = "var-surfaceMuted"; modeId = "light-mode"; value = @{ r = 0.157; g = 0.157; b = 0.165; a = 1 } },
        @{ variableId = "var-success"; modeId = "light-mode"; value = @{ r = 0.114; g = 0.620; b = 0.459; a = 1 } },
        @{ variableId = "var-warning"; modeId = "light-mode"; value = @{ r = 0.937; g = 0.624; b = 0.153; a = 1 } },
        @{ variableId = "var-error"; modeId = "light-mode"; value = @{ r = 0.886; g = 0.294; b = 0.290; a = 1 } }
    )
} | ConvertTo-Json -Depth 10

$response = Invoke-RestMethod -Uri "$BASE_URL/files/$FILE_KEY/variables" -Method POST -Headers $headers -Body $colorCollectionBody
Write-Host "Color Variables 등록 완료!" -ForegroundColor Green
Write-Host ($response | ConvertTo-Json -Depth 5)

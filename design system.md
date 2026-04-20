# Sapiens 디자인 시스템 가이드

Compose Material3 기반 Android 앱(`app`)의 시각 토큰·타이포·간격·모서리 규약을 정리한 문서입니다. 구현 소스는 `app/src/main/java/com/sapiens/app/ui/theme/` 및 `ui/common/ChipColors.kt`를 기준으로 합니다.

---

## 1. 원칙

- **하드코딩 지양**: 색상 `#…`, `NN.dp`, 임의 `RoundedCornerShape(N.dp)` 대신 테마 토큰을 사용합니다.
- **이중 소스**: 화면 전역 색은 `MaterialTheme.colorScheme`과 커스텀 런타임 색(`Background`, `Card`, `TextPrimary` 등)을 함께 쓰는 구조입니다. 신규 화면은 가능하면 시맨틱 토큰을 우선합니다.
- **폰트**: `Typography.kt` 주석대로 SUIT 예정이며, 현재는 `FontFamily.SansSerif`입니다.

---

## 2. 파일 맵

| 파일 | 역할 |
|------|------|
| `Theme.kt` | `SapiensTheme`, M3 `darkColorScheme` / `lightColorScheme`, `SapiensTypography` 연결 |
| `Color.kt` | 브랜드·팔레트·시맨틱·칩 팔레트·런타임 getter·`applyThemePalette` |
| `Typography.kt` | M3 `Typography` 전역 스케일 (`SapiensTypography`) |
| `Type.kt` | `SapiensTextStyles` — 화면별 파생 스타일 |
| `Spacing.kt` | `Spacing` 객체 — 간격 dp 토큰 |
| `Shape.kt` | `AppRadius`, `AppShapes` — 모서리 반경·프리셋 |
| `Dimens.kt` | 카드·시트 레이아웃용 복합 상수 (Spacing 기반) |
| `common/ChipColors.kt` | 뉴스 카테고리 문자열 → `CategoryChipPalette` 매핑 |

---

## 3. 색상 (Color)

### 3.1 브랜드·온브랜드

| 토큰 | Hex | 설명 |
|------|-----|------|
| `Primary` | `#F56E0F` | 주황 브랜드 |
| `Accent` | `Primary`와 동일 (getter 별칭) | |
| `OnPrimaryFixed` | `#FFFFFF` | Primary 위 고정 대비 텍스트/아이콘 |

### 3.2 다크 팔레트 (정적)

| 토큰 | Hex |
|------|-----|
| `BackgroundDark` | `#151419` |
| `CardDark` | `#1B1B1E` |
| `ElevatedDark` | `#262626` |
| `TextPrimaryDark` | `#FBFBFB` |
| `TextSecondaryDark` | `#878787` |
| `HairDark` | `#26FFFFFF` |
| `UpDark` | `#FF3B30` |
| `DownDark` | `#0064FF` |

### 3.3 라이트 팔레트 (정적)

| 토큰 | Hex |
|------|-----|
| `BackgroundLight` | `#FAFAF8` |
| `CardLight` | `#FFFFFF` |
| `ElevatedLight` | `#F2F1EE` |
| `TextPrimaryLight` | `#1A1A1A` |
| `TextSecondaryLight` | `#6B6B6B` |
| `HairLight` | `#14000000` |
| `UpLight` | `#E0291F` |
| `DownLight` | `#0052CC` |

### 3.4 시맨틱 (라이트/다크 공통으로 쓰는 조각)

| 토큰 | Hex | 비고 |
|------|-----|------|
| `Success` | `#1D9E75` | |
| `SuccessContainer` | `#261D9E75` | 알파 포함 |
| `Warning` | `#EF9F27` | |
| `WarningContainer` | `#26EF9F27` | |
| `Error` | `#E24B4A` | |
| `ErrorContainer` | `#26E24B4A` | |
| `SuccessBright` | `#22C55E` | 강조용 녹색 |
| `SurfaceMuted` | `#28282A` | |
| `SurfaceChartInactive` | `#3C3C3C` | |
| `SurfaceOpinion` | `#1F1D24` | |
| `SurfaceHairlineOnDark` | `#14FFFFFF` | |
| `DividerOnMutedSurface` | `White @ 6%` | |
| `MarketFlat` | `#878787` | |

### 3.5 런타임 테마 색 (`applyThemePalette`와 연동)

앱에서 getter로 읽는 값: `Background`, `Card`, `Elevated`, `TextPrimary`, `TextSecondary`, `TextTertiary`, `Hair`, `MarketUp`, `MarketDown`.

- `TextTertiary`: 다크에서는 `TextSecondaryDark`, 라이트에서는 `TextSecondaryLight`에 알파 0.85 적용 값.

### 3.6 Material3 `ColorScheme` 매핑 (`Theme.kt`)

- **Primary / onPrimary**: `Primary`, `OnPrimaryFixed`
- **background / onBackground**: 다크·라이트 각 팔레트의 Background / TextPrimary 계열
- **surface / onSurface**: background와 동일 계열로 정렬
- **surfaceVariant / onSurfaceVariant**: 카드·보조 텍스트 계열
- **surfaceContainer / surfaceContainerHigh**: 카드·Elevated
- **outline**: Hair 계열

### 3.7 뉴스 카테고리 칩 — `CategoryChipPalette`

경제·IT·정치·사회·국제·부동산·산업·금융·매크로·default 등 배경/전경 쌍이 정의되어 있습니다. UI에서는 `categoryChipColors(label)` (`ChipColors.kt`)로 문자열을 매핑합니다.

### 3.8 마켓 섹터 칩 — `SectorChipPalette.colors(sector)`

섹터 이름별 `(배경, 전경)` 쌍. 미매칭 시 기본 블루 톤 쌍을 사용합니다.

---

## 4. 타이포그래피 (Typography)

폰트 패밀리: **SansSerif** (추후 SUIT). 전역은 `MaterialTheme.typography` → `SapiensTypography`.

| 스타일 | fontWeight | fontSize | lineHeight | letterSpacing |
|--------|------------|------------|--------------|----------------|
| displayLarge | Bold | 57sp | 64sp | -0.25sp |
| displayMedium | Bold | 45sp | 52sp | 0 |
| displaySmall | Bold | 36sp | 44sp | 0 |
| headlineLarge | Bold | 32sp | 40sp | 0 |
| headlineMedium | Bold | 28sp | 36sp | 0 |
| headlineSmall | Bold | 24sp | 32sp | 0 |
| titleLarge | SemiBold | 22sp | 28sp | 0 |
| titleMedium | SemiBold | 16sp | 24sp | 0.15sp |
| titleSmall | SemiBold | 14sp | 20sp | 0.1sp |
| bodyLarge | Medium | 16sp | 24sp | 0.15sp |
| bodyMedium | Medium | 14sp | 20sp | 0.25sp |
| bodySmall | Medium | 12sp | 16sp | 0.4sp |
| labelLarge | SemiBold | 14sp | 20sp | 0.1sp |
| labelMedium | Medium | 12sp | 16sp | 0.5sp |
| **labelSmall** | Medium | **11sp** | 16sp | 0.5sp |

태그·뱃지·캡션 등 작은 라벨은 **`labelSmall`** 또는 아래 **`SapiensTextStyles`**를 우선 검토합니다.

### 4.1 `SapiensTextStyles` (`Type.kt`)

| 이름 | 기반 | 주요 조정 |
|------|------|------------|
| `briefingPublisherChip` | labelSmall | 11sp / lineHeight 14sp |
| `morningCardHeadline` | bodyMedium | 13sp / Medium |
| `morningListRow` | bodyMedium | 13sp |
| `briefingThemeHeadline` | headlineMedium | 24sp / lineHeight 34sp |
| `marketIndexGroup` | labelSmall | 9sp |
| `statCaption9` | labelSmall | 9sp |
| `toggleLabel12` | labelMedium | 12sp |

---

## 5. 간격 (Spacing)

`Spacing` 객체 — **4dp 그리드**를 기준으로 하되, 제품 요구에 맞춘 보조 스텝을 포함합니다.

| 토큰 | dp | 비고 |
|------|-----|------|
| `space0` | 0 | |
| `hairline` | 0.5 | 보더·구분선 두께 등 |
| `space1` ~ `space6` | 1–6 | |
| `space8` | 8 | |
| `space10` | 10 | |
| `space12` | 12 | |
| `space14` | 14 | |
| `space16` | 16 | 시트 좌우 등 |
| `space18` | 18 | `RowVertical` 등 |
| `space20` | 20 | |
| `space22` | 22 | |
| `space24` | 24 | 카드 좌우 패딩 등 |
| `space26` | 26 | |
| `space28` | 28 | |
| `space30` | 30 | |
| `space32` | 32 | 바텀시트 하단 등 |
| `space36` | 36 | |
| `space40` | 40 | |
| `space44` | 44 | |
| `space48` | 48 | |
| `space52` | 52 | |
| `space56` | 56 | |
| `space64` | 64 | |
| `space76` | 76 | 최소 너비 등 |
| `space332` | 332 | 국내 브리핑 페이저 고정 높이 |

### 5.1 레이아웃 상수 (`Dimens.kt`)

| 상수 | 값 | 용도 |
|------|-----|------|
| `CardPaddingHorizontal` | `space24` | 테마 카드 좌우 |
| `CardPaddingVertical` | `space30` | 테마 카드 상단 |
| `CardPaddingBottom` | `space26` | 테마 카드 하단 |
| `CardSpacing` | `space16` | 카드 내부 블록 간격 |
| `RowVertical` | `space18` | 리스트 행 세로 패딩 |
| `SheetHorizontal` | `space16` | 시트 좌우 |
| `BottomSheetBottomPadding` | `space32` | 모달 시트 콘텐츠 하단 |
| `SheetBottom` / `SheetTop` | 16 / 24 | 시트 내부 |
| `SummaryPointSpacing` | `space12` | 요약 포인트 간격 |
| `SheetDragHandleHeight` | `space5` (5dp) | 드래그 핸들 높이 |

---

## 6. 모서리 (Shape)

### 6.1 `AppRadius` (dp)

2, 3, 4, 6, 8, 10, 12, 14, 15, 16, 18, 20, 99

### 6.2 `AppShapes` — 용도 가이드

| 프리셋 | 반경 요약 | 권장 용도 |
|--------|-----------|-----------|
| `chip` | 6dp | 일반 칩·태그 |
| `chipTight` | 4dp | 조밀한 칩 |
| `button` | 12dp | 주요 버튼 |
| `cardMedium` | 12dp | 시트·패널 안쪽 카드 |
| `card` | 18dp | 메인 카드 |
| `cardNested` | 14dp | 중첩 카드·그리드 셀 |
| `sheetHandle` | 99dp (캡슐) | 바텀시트 드래그 핸들 |
| `pill` / `pillInner` | 15 / 10dp | 토글·필 형태 |
| `thumbnail` | 6dp | 썸네일 |
| `barTop` | 상단 3dp, 하단 0 | 상단 바 세그먼트 |
| `barSegment` | 3dp | 작은 바 |
| `hairlineTrack` | 2dp | 트랙 |
| `healthCapsule` | 20dp | 건강/상태 캡슐 |
| `panel` | 16dp | 패널·차트 컨테이너 |
| `searchField` | 8dp | 검색 필드·작은 표면 |

---

## 7. 구현 시 체크리스트

1. 색: `Color(0xFF…)` 신규 추가보다 `Color.kt`에 토큰 정의 후 화면에서 참조.
2. 간격: `12.dp` → `Spacing.space12` (또는 `Dimens`에 이미 있는 복합 상수).
3. 모서리: `RoundedCornerShape(12.dp)` → `AppShapes.button` 또는 `AppShapes.cardMedium` 등 의미에 맞는 프리셋.
4. 타이포: 화면별 미세 조정이 반복되면 `SapiensTextStyles`에 한 곳으로 모으기.
5. 다크/라이트 전환: `applyThemePalette(darkTheme)` 호출 지점과 동기화.

---

## 8. 변경 이력

문서는 코드와 함께 유지보수합니다. 토큰 추가·이름 변경 시 본 파일과 `Color.kt` / `Spacing.kt` / `Shape.kt` / `Typography.kt` / `Type.kt`를 함께 갱신하세요.

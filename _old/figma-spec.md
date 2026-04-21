# 브리핑 화면 Figma 스펙

Compose **1dp = Figma 1px**(@1x) 기준으로 정리했습니다. 색·타이포는 앱에서 쓰는 **의미 토큰 이름**과, Figma 로컬 스타일/변수와 매핑하기 쉬운 **권장 Figma 이름**을 병기합니다.

## 소스 범위

| 영역 | 경로 |
|------|------|
| 브리핑 본문 | `app/src/main/java/com/sapiens/app/ui/briefing/BriefingScreen.kt`, `BriefingComponents.kt` |
| 상·하단 크롬(탭 0일 때) | `app/src/main/java/com/sapiens/app/ui/main/MainScreen.kt` (`MainTopAppBar`, `BottomNavigationBar`, `Scaffold`) |
| 기사 시트(브리핑에서 기사 탭 시) | `app/src/main/java/com/sapiens/app/ui/common/ArticleBottomSheet.kt` (`kind = News`) |
| 토큰 | `ui/theme/Color.kt`, `Spacing.kt`, `Shape.kt`, `Typography.kt`, `Type.kt`, `Dimens.kt` |

`BriefingViewModel.kt`, `BriefingDomesticMerge.kt`는 UI 스펙 없음(데이터만).

---

## 전체 계층 (브리핑 탭 선택 시)

```
Scaffold (containerColor = Background)
├── topBar: MainTopAppBar
│   └── [statusBarsPadding + top 6dp]
│       └── title: Text ("브리핑" = tabs[0].label)
│       └── (브리핑 탭에서는 actions 없음)
├── bottomBar: BottomNavigationBar
│   └── NavigationBar (4× NavigationBarItem)
└── content: Box (fillMaxSize, background Background, padding innerPadding)
    └── BriefingScreen: Box(fillMaxSize)
        ├── [로딩 시] 로딩 레이어
        └── [완료 시] LazyColumn
            ├── SectionLabel ("국내 주요뉴스")
            ├── DomesticBriefingMixedCard (+ bottom padding 8dp)
            ├── Spacer height 18dp
            ├── SectionLabel ("해외 주요뉴스")
            └── USMajorArticlesCard
        └── [선택 시 오버레이] ArticleBottomSheet (ModalBottomSheet)
```

---

## 색상 토큰 (다크 기본)

| 토큰 (Kotlin) | 용도 | 정적 다크 값 (참고) |
|---------------|------|---------------------|
| `Background` | 스캐폴드·본문 배경 | `#151419` |
| `Card` | M3 `Card`/`ModalBottomSheet` 컨테이너 내부 컬럼 배경 | `#1B1B1E` |
| `TextPrimary` | 본문·제목 | `#FBFBFB` |
| `TextSecondary` | 캡션·구분선 알파 베이스 | `#878787` |
| `Accent` / `Primary` | 브랜드 주황 | `#F56E0F` |
| `OnPrimaryFixed` | Accent 위 텍스트(시트 버튼 등) | `#FFFFFF` |
| `MarketUp` / `MarketDown` / `MarketFlat` | (브리핑 폴더 내 미사용 카드 제외) 시장 색 | `#FF3B30` / `#0064FF` / `#878787` |
| `SurfaceMuted` | (동일) 중첩 카드 배경 | `#28282A` |

알파만 명시된 오버레이:

| 표현 | 값 |
|------|-----|
| `TextSecondary.copy(alpha = 0.22f)` | MorningSourceCard 보더 (해당 컴포넌트는 현재 브리핑 스크린 미사용) |
| `TextSecondary.copy(alpha = 0.2f)` | 브리핑 카드 `HorizontalDivider` |
| `TextSecondary.copy(alpha = 0.24f)` | MorningCardPager 구분선 (미사용) / ArticleBottomSheet 구분선 |
| `TextSecondary.copy(alpha = 0.4f)` | (미사용) 페이저 도트 비선택 |
| `Accent.copy(alpha = 0.14f)` | `BriefingPublisherChip`, `SourceChip` 배경 |

**Material `colorScheme.surface`**: `MorningSourceCard`만 사용. 테마상 다크에서 **`BackgroundDark`와 동일 값**(`Theme.kt`의 `surface = BackgroundDark`).

---

## 타이포 토큰 매핑

앱은 `MaterialTheme.typography` + `SapiensTextStyles`(`Type.kt`)를 혼용합니다. Figma에서는 기존 로컬 스타일 이름과 맞추기 쉽게 **권장 Figma 스타일**을 적었습니다.

| 앱 참조 | 내용 (주요 수치) | 권장 Figma Text Style |
|---------|------------------|------------------------|
| `MaterialTheme.typography.titleSmall` | 14sp / 20lh, SemiBold | `Title/Small` |
| `MaterialTheme.typography.titleLarge` + **+6sp**, **Bold** | 앱바 제목: (22+6)=**28sp**, Bold, `TextPrimary` | `Title/Large` + 크기 28sp·Bold로 오버라이드(앱과 동일) |
| `MaterialTheme.typography.bodyLarge` | 16sp / 24lh, Medium | `Body/Large Medium` |
| `MaterialTheme.typography.bodyMedium` | 14sp / 20lh, Medium | `Body/Medium Medium` |
| `MaterialTheme.typography.bodySmall` | 12sp / 16lh, Medium | `Body/Small Medium` |
| `MaterialTheme.typography.labelSmall` | 11sp / 16lh, Medium | `Label/Small` |
| `MaterialTheme.typography.labelLarge` | 14sp / 20lh, SemiBold | `Label/Large` |
| `MaterialTheme.typography.headlineMedium` (News 시트 헤드만 -1sp/-1lh) | 27sp / 35lh, Bold | `Heading/H3` 근사 + 1단계 조정 |
| `SapiensTextStyles.briefingPublisherChip` | `labelSmall` 기반 **11sp / 14lh** | `Briefing/Publisher` |
| `SapiensTextStyles.briefingThemeHeadline` | `headlineMedium` 기반 **24sp / 34lh**, Bold | `Briefing/Headline` |
| `SapiensTextStyles.morningCardHeadline` | 13sp, bodyMedium 기반 Medium | `Morning/Headline` (미사용 경로) |
| `SapiensTextStyles.morningListRow` | 13sp bodyMedium | (미사용) |
| `SapiensTextStyles.marketIndexGroup` | 9sp labelSmall 기반 | `Market Index Label` (미사용) |

---

## 간격·크기 토큰 (`Spacing.kt`)

| 토큰 | dp |
|------|-----|
| `hairline` | **0.5** (구분선 두께) |
| `space1` | 1 |
| `space2` | 2 |
| `space3` | 3 |
| `space4` | 4 |
| `space5` | 5 |
| `space6` | 6 |
| `space8` | 8 |
| `space10` | 10 |
| `space12` | 12 |
| `space14` | 14 |
| `space16` | 16 |
| `space18` | 18 |
| `space20` | 20 |
| `space22` | 22 |
| `space24` | 24 |
| `space26` | 26 |
| `space28` | 28 |
| `space30` | 30 |
| `space32` | 32 |
| `space44` | 44 |
| `space52` | 52 |
| `space56` | 56 |
| `space64` | 64 |

`Dimens.kt`: `CardPaddingHorizontal` = **24**, `CardPaddingVertical` = **30**, `CardPaddingBottom` = **26**, `RowVertical` = **18**, `SheetHorizontal` = **16**, `SheetTop` = **24**, `SheetDragHandleHeight` = **5**, `BottomSheetBottomPadding` = **32**, `SummaryPointSpacing` = **12**.

---

## 코너·모양 (`AppShapes`)

| 토큰 | 반경 |
|------|------|
| `AppShapes.card` | **18dp** 전체 |
| `AppShapes.cardNested` | **14dp** |
| `AppShapes.button` | **12dp** |
| `AppShapes.chip` | **6dp** |
| `AppShapes.chipTight` | **4dp** |
| `AppShapes.thumbnail` | **6dp** |
| `AppShapes.sheetHandle` | **99dp** (캡슐) |

---

# UI 요소별 스펙

아래 표에서 **부모**는 직계 상위를 뜻합니다. **크기**는 명시가 없으면 `fillMaxWidth`(가로 100%) 또는 콘텐츠 hug입니다.

---

## 1. `Scaffold` (브리핑 탭)

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `MainScreen` → Material3 `Scaffold` |
| **크기** | 화면 전체 |
| **배경색 토큰** | `Background` |
| **테두리** | 없음 |

---

## 2. `MainTopAppBar` — 상단 앱 바 (브리핑 탭 제목)

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `MainTopAppBar` → `TopAppBar` |
| **크기** | `Modifier.statusBarsPadding()` + 상단 **`padding(top = 6dp)`** (가로는 화면 전체). 제목 행 높이는 Material3 Small **TopAppBar 기본**(구현상 명시 없음, 일반적으로 **최소 64dp** 영역 + 상태바). |
| **패딩** | 상단 **6dp** 추가만 명시. 좌우 기본 앱 바 패딩(M3 기본, 보통 **16dp**). |
| **배경색 토큰** | `TopAppBarDefaults.topAppBarColors(containerColor = Background)` |
| **텍스트** | `Text`: `TextPrimary`, **28sp**(= titleLarge 22sp + 6), **Bold**, line height는 `titleLarge` 기반 **28sp**에서 폰트만 키움(`copy(fontSize = …+6f).sp`) |
| **타이포 토큰** | `MaterialTheme.typography.titleLarge` +6sp 오버라이드 (위 타이포 표 참고) |
| **테두리** | 없음 |
| **아이콘** | 브리핑 탭에서는 **actions 비표시** → 앱 바 아이콘 없음 |
| **부모** | `Scaffold.topBar` |

---

## 3. `BottomNavigationBar` — 하단 탭

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `BottomNavigationBar` → `NavigationBar` |
| **크기** | 가로 화면 전체. 세로는 **Material3 `NavigationBar` 기본 높이**(구현 미지정, 보통 **80dp**). |
| **배경색 토큰** | `NavigationBar(containerColor = Background)` |
| **탭 항목** | `NavigationBarItem` × 4. **선택**: `Accent` / **비선택**: `TextSecondary` (아이콘·라벨 동일). |
| **인디케이터** | `NavigationBarItemDefaults.colors(indicatorColor = Transparent)` → **선택 배경 필 없음** |
| **아이콘 (res/drawable)** | 순서: **`R.drawable.ico_brief`**, `ico_news`, `ico_market`, `ico_my`**. 크기: 명시 없음 → **M3 기본 24dp** 가정. |
| **라벨 타이포** | `Text`에 별도 `style` 없음 → M3 NavigationBarItem **기본 라벨 스타일**(보통 `labelMedium` 계열) |
| **부모** | `Scaffold.bottomBar` |

---

## 4. `BriefingScreen` — 루트

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `BriefingScreen` |
| **크기** | `Box(Modifier.fillMaxSize())` |
| **배경** | (직접 배경 없음) 부모 `Box`가 `Background` |
| **부모** | `MainScreen` content `Box` |

---

## 5. 로딩 상태 (`BriefingScreen`)

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `Box` + 중앙 `CircularProgressIndicator` |
| **크기** | `fillMaxSize`, 콘텐츠 정렬 Center |
| **배경색 토큰** | `Background` |
| **인디케이터 색** | `Accent` |
| **인디케이터 크기** | 명시 없음 → **M3 기본 지름 40dp** (라이브러리 기본) |
| **아이콘 drawable** | 없음 (벡터 프리미티브) |
| **부모** | `BriefingScreen` `Box` |

---

## 6. `LazyColumn` (본문 리스트)

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `LazyColumn` |
| **크기** | `fillMaxSize()` |
| **배경색 토큰** | `Background` |
| **contentPadding** | 세로만 **`vertical = 8dp`** (`Spacing.space8`) |
| **부모** | `BriefingScreen` `Box` |

---

## 7. `SectionLabel`

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `SectionLabel` |
| **크기** | `Column` — 가로는 자식에 맡김, 기본 fill 제약은 LazyColumn 아이템 폭(화면폭). |
| **패딩** | `Modifier.padding(horizontal = 20dp, vertical = 12dp)` |
| **부모-자식** | `SectionLabel` → `SectionTitleText` (`Text`) → [옵션] `subtitle` `Text` |
| **자식: `SectionTitleText`** | `title`: `MaterialTheme.typography.titleSmall`, `TextPrimary` |
| **자식: subtitle** (기본 브리핑에서는 `null`) | `bodyMedium`, `TextPrimary`, 상단 타이틀 대비 **`padding(top = 4dp)`** |

---

## 8. `DomesticBriefingMixedCard` — 국내 주요뉴스 카드

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `DomesticBriefingMixedCard` |
| **크기** | `Card`: `fillMaxWidth()` + **`padding(horizontal = 16dp)`** (카드 바깥). 내부 `Column`: 좌우 **24dp**, 상하 **26dp** 패딩. |
| **배경색 토큰** | `Card` M3 기본 컨테이너 + 내부 `Column.background(Card)` 이중이나 색은 동일 토큰 `Card`. |
| **모양** | `shape = AppShapes.card` → **코너 반경 18dp**. |
| **테두리** | 코드상 **Border 없음**. 음영은 **M3 `Card` 기본 elevation**(프로젝트에서 `CardDefaults` 미커스터마이즈). |
| **부모-자식** | `Card` → `Column` → (`BriefingPublisherChip` → Spacer **8dp** → 헤드라인 `Row`) → [조건부] `HorizontalDivider` → 반복 `Row`(리스트 행) |

### 8a. `BriefingPublisherChip` (private)

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `BriefingPublisherChip` |
| **크기** | Hug. `Surface` + 내부 `Text` **`padding(horizontal = 6dp, vertical = 2dp)`** |
| **배경색** | `Accent.copy(alpha = 0.14f)` (토큰 혼합) |
| **모양** | `AppShapes.chipTight` → **4dp** |
| **텍스트** | `SapiensTextStyles.briefingPublisherChip`, 색 **`Accent`** |
| **테두리** | 없음 |

### 8b. 첫 기사 헤드라인 행

| 항목 | 값 |
|------|-----|
| **레이아웃** | `Row(fillMaxWidth, clickable)`, `verticalAlignment = Top` |
| **텍스트** | `SapiensTextStyles.briefingThemeHeadline`, `TextPrimary`, max 2줄, Ellipsis, `weight(1f)` |

### 8c. 구분선 (첫 기사 vs 리스트)

| 항목 | 값 |
|------|-----|
| **컴포넌트** | `HorizontalDivider` |
| **색** | `TextSecondary` **alpha 0.2** |
| **두께** | **`hairline` = 0.5dp** |
| **마진** | `padding(top = 6+2=8dp, bottom = 6dp)` |

### 8d. 나머지 기사 행 (`articles.drop(1)`)

| 항목 | 값 |
|------|-----|
| **레이아웃** | `Row` + `clickable` + **`padding(vertical = 8dp)`** |
| **텍스트** | `MaterialTheme.typography.bodyLarge`, `TextPrimary`, 1줄 Ellipsis, `weight(1f)` |
| **행 사이 구분선** | 동일 `HorizontalDivider` (0.2 alpha, 0.5dp), **`padding(vertical = 6dp)`** |

---

## 9. 섹션 간격 (`BriefingScreen`)

| 항목 | 값 |
|------|-----|
| **컴포넌트** | `Spacer` |
| **크기** | **`height = 18dp`** (`Spacing.space18`) — 국내 카드 블록과 해외 섹션 라벨 사이 |
| **카드 바깥 하단** | `DomesticBriefingMixedCard`에 **`Modifier.padding(bottom = 8dp)`** |

---

## 10. `USMajorArticlesCard` — 해외 주요뉴스 카드

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `USMajorArticlesCard` |
| **크기·패딩·모양** | **`DomesticBriefingMixedCard`와 동일 패턴**: 카드 좌우 바깥 **16dp**, 내부 **24h / 26v**, `AppShapes.card` **18dp**, 배경 `Card`, 구분선·리스트 행 규칙 동일. |
| **빈 상태** | 단일 `Text`: "불러온 기사가 없습니다.", `bodyMedium`, `TextSecondary`, **`padding(vertical = RowVertical)` = 18dp** |
| **기사 있을 때** | 첫 줄 `BriefingPublisherChip` → 동일 헤드라인/리스트/디바이더 구조 (**해외** 라벨은 `source` 기본 `"해외"`). |
| **부모-자식** | `Card` → `Column` → (빈/목록 분기) |

---

## 11. `ArticleBottomSheet` (브리핑에서 `ArticleBottomSheetKind.News`)

| 항목 | 값 |
|------|-----|
| **컴포넌트 이름** | `ModalBottomSheet` + 내부 `Column` |
| **크기** | `fillMaxWidth`, 하단 `navigationBarsPadding()` |
| **시트 컨테이너 색** | `containerColor = Card` |
| **드래그 핸들** | `dragHandle = null` — 대신 상단 **커스텀 핸들** `Box`: 상단 **`padding(top = 8dp)`**, 높이 **`SheetDragHandleHeight` = 5dp**, 가로 **`fillMaxWidth(0.12f)`** (= 화면의 **12%**), 배경 `TextSecondary` **0.5** 알파, 모양 **`AppShapes.sheetHandle`** |
| **콘텐츠 패딩** | 가로 **`SheetHorizontal` = 16dp**, 하단 **`BottomSheetBottomPadding` = 32dp** |
| **부모-자식** | `Column` → Handle `Box` → 메타 `Row` → 헤드라인 `Text` → `HorizontalDivider` → 요약 bullet `Column` → 버튼 `Row` |

### 11a. 메타 행

| 항목 | 값 |
|------|-----|
| **레이아웃** | `Row`, `SpaceBetween`, `padding(top = SheetTop)` = **24dp** |
| **좌** | `"${source} · ${time}"`, `bodySmall`, `TextSecondary` |
| **우** | `CategoryChip` (로컬 private, `AppShapes.chip` 6dp, `labelSmall`, `categoryChipColors` 매핑 — `Color.kt` `CategoryChipPalette`) |

### 11b. 헤드라인 (News)

| 항목 | 값 |
|------|-----|
| **텍스트** | `headlineMedium`에서 **fontSize -1sp, lineHeight -1sp** → **27sp / 35lh**, Bold, `TextPrimary`, max 3줄 |
| **패딩** | **`padding(top = 14dp)`** |

### 11c. 구분선

| 항목 | 값 |
|------|-----|
| **색** | `TextSecondary` **alpha 0.24** |
| **두께** | 명시 없음 → **M3 `HorizontalDivider` 기본 1dp** |

### 11d. 요약 불릿

| 항목 | 값 |
|------|-----|
| **레이아웃** | `Column`, `padding(top = 14dp)`, 항목 간격 **`SummaryPointSpacing` = 12dp** |
| **각 행** | `Row`, 아이콘 없음. 번호 `"${n}."` → `labelLarge` + **SemiBold**, 색 **`Accent`**. 본문 → `bodyMedium`, `TextSecondary` |
| **항목 수** | 최대 **4**개 (`resolvedPoints.take(4)`) |

### 11e. 하단 버튼 행 (News만)

| 항목 | 값 |
|------|-----|
| **레이아웃** | `Row`, `padding(top = 20dp)`, 항목 간격 **6dp** |
| **왼쪽 `OutlinedButton`** | 높이 **64dp**, `weight(1f)`, `shape = AppShapes.button` (**12dp**), 테두리 **`BorderStroke(1dp, TextSecondary 0.45 alpha)`**, 라벨 "기사 원문 보기", `labelLarge`, `TextPrimary` / 비활성 시 `TextSecondary` 0.45 |
| **오른쪽 `NewsSaveBookmarkButton`** | 높이 **64dp**, `weight(1f)`, 배경 **`Accent`** (눌림 시 살짝 어둡게), 글자 **`OnPrimaryFixed`**, 라벨 "저장" / "저장 취소", `labelLarge` + SemiBold, **그림자 0** (`buttonElevation` 전부 0) |

---

# 동일 패키지의 기타 컴포넌트 (`BriefingComponents.kt`, 현재 `BriefingScreen`에서 미사용)

Figma에 “브리핑 모듈 전체”를 옮길 때 참고용입니다.

| 컴포넌트 | 요약 스펙 |
|----------|-----------|
| **`MorningSourceCard`** | `Surface`, `AppShapes.button` (**12dp**), `BorderStroke(hairline, TextSecondary 0.22)`, 내부 패딩 `CardPaddingHorizontal`(24) / `CardPaddingVertical`(30) / `CardPaddingBottom`(26), 배경 `colorScheme.surface`. 상단 `SourceChip` + 시간(`briefingPublisherChip`). 헤드라인+`HeadlineThumbnail` **52×52**, `morningCardHeadline`. 썸네일 없을 때: 플레이스홀더 배경 `TextSecondary 0.16`, **`Icons.Default.Image` 22dp** (앱 `res` 아님). Coil placeholder/error: **`android.R.drawable.ic_menu_report_image`**. 구분선 0.2 alpha hairline. |
| **`MorningCardPager`** | `Card` 16dp 바깥 패딩, 내부 패딩 `CardPadding*`, 페이저 + `titleLarge`(2줄 고정 박스 min **56dp**) + spacer **`RowVertical` 18dp** + 요약 `bodyMedium`(min **44dp**) + 구분선(0.24 alpha, hairline, vertical **18dp**) + 메타 `bodySmall` + **`CategoryChip`**. 하단 페이지 인디케이터: **`padding(top = 12dp)`**, 도트 **`height 6dp`**, 간격 **`horizontal 3dp`**, 선택 시 **`width 20dp`** / 비선택 **6dp**, `CircleShape`, 선택 `Accent` / 비선택 `TextSecondary` **0.4**. |
| **`MarketTickerBanner`** | 세로 **8dp** 바깥 패딩, 좌우 **16dp**. 2열 `MarketTickerItem` 사이 **세로 구분선**: **28dp** 높, **0.5dp** 너비, 색 `TextSecondary` 0.2. 아이템 행 **좌우 10dp** 패딩. 하단 인디케이터: **`AppShapes.sheetHandle`**, 높이 **4dp**, 간격 **2dp**, 선택 너비 **12dp** / 비선택 **4dp**, 색 동일 패턴(선택 Accent, 비선택 TextSecondary **0.35**). |
| **`MarketTickerItem`** | 이름 `labelSmall` `TextSecondary`, 값 `bodyMedium` Medium `TextPrimary`, 변동 `labelSmall` + `MarketUp`/`MarketDown`/`MarketFlat`. |
| **`MarketIndexGrid`** | 좌우 **16dp**, 행 간격 **8dp**, 열 간격 **8dp**. |
| **`MarketIndexCard`** | 배경 **`SurfaceMuted`**, `AppShapes.cardNested` (**14dp**), 패딩 `CardPadding*`, 내부 간격 **6dp**. 그룹명 `SapiensTextStyles.marketIndexGroup` `TextSecondary`, 지수명 `labelSmall`, 값 `titleMedium` Bold, 등락 `MarketIndexStyleChangeText`(`labelSmall` + 방향색). |
| **`SourceChip`** | `Accent` 0.14 배경, `AppShapes.chip` (**6dp**), 내부 **`padding(8h, 3v)`**, 점 **5×5** 원 + `Accent`, 텍스트 `labelSmall` `Accent`. |
| **`CategoryChip`** (파일 내 private, MorningCardPager용) | `categoryChipColors`, `AppShapes.chip`, 텍스트 `labelSmall`, **`padding(8h, 3v)`**. |

---

## 아이콘·drawable 정리 (브리핑 플로우)

| 용도 | 리소스 / 출처 | 크기(명시) |
|------|----------------|------------|
| 하단 탭「브리핑」 | **`res/drawable/ico_brief.xml`** (또는 png) | M3 기본 **24dp** |
| 기사 시트 북마크 | Material **`Icons.Filled.Bookmark` / `Icons.Outlined.BookmarkBorder`** | **24dp** (`Spacing.space24`) |
| 썸네일 플레이스홀더 (MorningSourceCard 경로) | Material **`Icons.Default.Image`** | **22dp** |
| Coil 이미지 fallback | **`android.R.drawable.ic_menu_report_image`** | 썸네일 프레임에 맞춤(보통 **52dp** 정사각) |

---

## Figma 재현 체크리스트

1. **프레임**: 전체 높이는 기기별; 가로 기준은 코드가 **전폭(fillMaxWidth)** 이므로 모바일 기준 **360px** 권장.  
2. **상단**: 상태바 영역 + **6dp** 추가 top, 배경 `Background`, 타이틀 **28sp Bold** `TextPrimary`.  
3. **리스트**: 배경 `Background`, 리스트 상하 **8dp** inset.  
4. **섹션 라벨**: 좌우 **20dp**, 상하 **12dp**, `Title/Small` + `TextPrimary`.  
5. **카드 두 종**: 바깥 좌우 **16dp**, 카드 코너 **18dp**, 내부 패딩 **24 / 26**, 배경 `Card`, 디바이더 **0.5dp**·`TextSecondary` **20%**, 칩 **4dp**·Accent 14% 배경·Accent 텍스트 11/14sp.  
6. **섹션 간**: **18dp** 스페이서 + 국내 카드 아래 **8dp**.  
7. **시트**: 컨테이너 `Card`, 핸들 **12%** 너비·**5dp** 높·캡슐, 좌우 **16**·하단 **32**, 헤드라인 **27/35sp**, 구분선 **1dp**·24% alpha, CTA **64dp** 고정 높이 한 줄.  

이 문서는 `briefing` 패키지와 브리핑 탭에 연결된 **`MainScreen` / `ArticleBottomSheet`** 구현을 기준으로 했으며, Material3 컴포넌트의 **일부 기본 치수**(내비 높이, `Card` 기본 elevation 등)는 라이브러리 기본값에 의존합니다.

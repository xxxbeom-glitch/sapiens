# UI 색상·타이포 매핑 (화면별)

기준: `Color.kt` 토큰명, `Typography.kt`의 `MaterialTheme.typography.*`, `Type.kt`의 `SapiensTextStyles.*`.

`ui/theme/Color.kt` 밖에서 **`Color(0x…)` 리터럴**이 없으면 hex 하드코딩은 없다고 본다. `CategoryChipPalette` / `SectorChipPalette`의 hex는 **토큰 정의**이지 화면 하드코딩이 아니다.

---

## 메인 (탭 셸)

`MainScreen.kt` — 하단 네비·상단 앱바.

### 색상

- **Background**: `Scaffold`·콘텐츠 영역·`TopAppBar` 배경
- **TextPrimary**: 상단 탭 제목
- **Accent**: 선택된 하단 탭 아이콘·라벨
- **TextSecondary**: 비선택 하단 탭 아이콘·라벨
- `Color.Transparent`: 탭 인디케이터 바 숨김 — 프레임워크 상수, 토큰 대체 불필요

### 텍스트 스타일

- **titleLarge** (+ `fontSize` +6sp, Bold): 상단 앱바 제목(브리핑·뉴스·마켓·마이)

---

## 브리핑

`BriefingScreen.kt`, `BriefingComponents.kt`.

### 색상

- **Background**: 화면·로딩 배경
- **Accent**: 로딩 인디케이터
- **Card**: 국내/해외 브리핑 카드 내부
- **TextPrimary**: 섹션 제목, 국내 헤드라인·본문, 해외 카드 제목 등
- **TextSecondary**: 보조 텍스트, 구분선·테두리 알파, 지수 보조 라벨
- **MarketUp** / **MarketDown** / **MarketFlat**: 지수 등락(`MarketIndexCard` 등)
- **SurfaceMuted**: 중첩 카드(지수 그리드 등)
- **CategoryChipPalette** (via `categoryChipColors`): 카테고리 칩 배경·전경
- `MaterialTheme.colorScheme.surface`: `MorningSourceCard` 카드 면 — **Card 토큰과 불일치 가능** → 아래 하드코딩 표 참고

### 텍스트 스타일

- **titleSmall**: 섹션 라벨 제목(`SectionLabel`)
- **bodyMedium**: 섹션 부제(`SectionLabel` subtitle)
- **SapiensTextStyles.briefingPublisherChip**: 국내 카드 언론사 칩
- **SapiensTextStyles.briefingThemeHeadline**: 국내 카드 메인 헤드라인
- **SapiensTextStyles.morningCardHeadline** / **morningListRow**: 아침/모닝 카드 블록
- **bodyLarge**: 국내 리스트 헤드라인·행
- **titleLarge**: 해외 카드 제목
- **bodyMedium** / **bodySmall**: 해외 카드 본문·메타
- **labelSmall**: 지수 이름·보조 수치
- **titleMedium**: 지수 값
- **SapiensTextStyles.marketIndexGroup**: `MarketIndexGrid` 등 그룹 라벨(메인 플로우에서 미노출일 수 있음)

---

## 뉴스

`NewsScreen.kt`, `NewsComponents.kt`.

### 색상

- **Background**: 화면·로딩 배경
- **Accent**: 상단 탭 강조, 로딩, 순위 1~3 숫자, 국내/해외 토글 선택 캡슐 배경
- **Card**: 피드 카드, 토글 비선택 쪽 반투명 배경(`Card.copy(alpha = 0.5f)`)
- **TextPrimary**: 탭 선택 라벨
- **TextSecondary**: 탭 비선택, 시간·언론사, 구분선 알파
- **OnPrimaryFixed**: 토글 선택 캡슐 위 라벨
- `Color.Transparent`: 토글 비선택 캡슐 배경 — 의도적 유지
- **CategoryChipPalette** (via `categoryChipColors`): 칩

### 텍스트 스타일

- **SapiensTextStyles.toggleLabel12**: “국내” / “해외” 토글
- **titleSmall**: 뉴스 제목(피드)
- **labelSmall**: 시간, 순위, 칩 라벨
- **bodySmall**: 언론사

---

## 마켓

`MarketScreen.kt`, `MarketSearchBottomSheet.kt`, `StockDetailBottomSheet.kt`.

### 색상

- **Background**: 화면·탭 행 배경
- **Accent**: 상단 탭 강조
- **Card**: 테마/업종 카드, 종목 상세 리포트·뉴스 리스트 카드
- **TextPrimary**: 해시태그 제목, 종목명, 가격, 로고 플레이스홀더 글자
- **TextSecondary**: 설명, 구분선, 로고 원형 배경 알파
- **TextTertiary**: 더보기/접기 링크
- **TextSecondary** + `ContentAlpha.hairlineOnSecondary`: 종목 행 구분선
- **ContentAlpha** (hairline 등): 상세 시트 구분선·아이콘 고스트
- **SurfaceMuted**: 투자지표·연간 재무 소카드
- **SectorChipPalette** (`colors()`): 업종 칩 배경·전경
- **MarketUp** / **MarketDown** / **MarketFlat**: 등락 텍스트(`MarketIndexStyleChangeText`)
- `Color.Black.copy(alpha = 0.6f)`: 검색 바텀시트 스크림 — **하드코딩**, 아래 표 참고

### 텍스트 스타일

- **titleLarge** (커스텀 lineHeight): 테마 카드 `#테마명`
- **bodySmall**: 테마 설명
- **labelSmall**: 더보기/접기, 로고 한 글자, 섹터 칩, 등락 수치
- **bodyMedium**: 종목명, 빈 상태 안내
- **titleMedium**: 가격, 지표·재무 수치
- **bodyLarge**: 로딩 실패 메시지
- **titleLarge** / **bodySmall**: 상세 헤더(종목·거래소)
- **titleSmall**: 섹션·피드 메타+헤드라인
- **SapiensTextStyles.marketIndexGroup**: 투자지표·연간 재무 그룹 라벨

---

## 마이

`MyScreen.kt`.

### 색상

- **Background**: 스크롤 영역 배경
- **Card**: 카드형 행 `Surface`
- **TextPrimary**: 제목·본문 강조
- **TextSecondary**: 부제, 로그아웃, 비선택 라디오, 구분선 알파
- **Primary**: 아이콘 틴트, API 모델 칩 선택 배경
- **OnPrimaryFixed**: 푸시 스위치 켠 상태 대비
- **TextSecondary** (알파 변형): 스위치 트랙·구분선

### 텍스트 스타일

- **labelLarge**: 일부 행 라벨
- **titleMedium**: 카드형 섹션 제목
- **bodyMedium** / **bodySmall**: 본문·부제
- **bodyLarge**: 긴 설명
- **labelSmall**: API 상태 보조

---

## 공통

`ArticleBottomSheet`, `CompanyBottomSheet`, `PdfViewerDialog`, `MarketIndexStyleChangeText`, `ChipColors`/`categoryChipColors`.

### 색상

- **ArticleBottomSheet — Card**: 시트 배경
- **ArticleBottomSheet — TextPrimary** / **TextSecondary**: 헤드라인, 요약, 메타, 핸들·구분선 알파
- **ArticleBottomSheet — Accent**: 요약 번호, 북마크(선택), 저장 버튼 배경
- **ArticleBottomSheet — OnPrimaryFixed**: 저장 버튼 라벨
- **ArticleBottomSheet — `Color.Transparent`**: 아웃라인 버튼 컨테이너(의도적)
- **ArticleBottomSheet — Accent.darkenTowardsBlack**: 저장 버튼 눌림(hex 아님, Accent 파생)
- **CompanyBottomSheet — TextPrimary** / **TextSecondary**: 제목·본문
- **CompanyBottomSheet — SurfaceMuted** / **SurfaceOpinion** / **SurfaceHairlineOnDark**: 차트·의견 패널
- **CompanyBottomSheet — Accent**: 강조 테두리·소제목
- **CompanyBottomSheet — DividerOnMutedSurface**: 구분선
- **CompanyBottomSheet — Success** / **Warning** / **Error** (+ Container): 건전성·상태 바
- **CompanyBottomSheet — `Color.Black.copy(alpha = 0.6f)`**: 스크림 — **하드코딩**, 아래 표 참고
- **PdfViewerDialog — Card** / **Background** / **TextPrimary** / **TextSecondary** / **Accent**: 툴바·본문·로딩
- **MarketIndexStyleChangeText — MarketUp** / **MarketDown** / **MarketFlat**: 등락 색
- **CategoryChipPalette**: 뉴스·브리핑·기사 시트 칩

### 텍스트 스타일

- **ArticleBottomSheet — bodySmall**: 출처·시간
- **ArticleBottomSheet — headlineMedium**: 헤드라인(News 종은 약간 축소 copy)
- **ArticleBottomSheet — labelLarge**: 요약 번호, 원문 버튼, 저장 버튼
- **ArticleBottomSheet — bodyMedium**: 요약 문장
- **ArticleBottomSheet — labelSmall**: 카테고리 칩
- **CompanyBottomSheet — labelSmall** / **titleSmall** / **bodyMedium**: 탭·지표·본문

---

## 하드코딩(hex·고정 컬러) 및 토큰 대체

| 위치 | 코드 | 대체 권장 |
|------|------|-----------|
| `MarketSearchBottomSheet`, `CompanyBottomSheet` | `Color.Black.copy(alpha = 0.6f)` | `Color.Black.copy(alpha = ContentAlpha.modalScrim)` 또는 `Color.kt`에 **ModalScrim** 추가 |
| `MorningSourceCard` | `MaterialTheme.colorScheme.surface` | 디자인 토큰과 맞추려면 **Card** (또는 **Elevated**) |
| `MainScreen` / `NewsScreen` / `ArticleBottomSheet` | `Color.Transparent` | 의도 유지(토큰화 불필요) |

`SectorChipPalette` / `CategoryChipPalette` 내부 `Color(0x…)`는 **Color.kt 설계 토큰**이므로 화면에서 별도 hex로 치환할 항목이 아님.

---

## Typography 역할 빠른 참고

- **display*** / **headline***: 큰 타이틀
- **titleLarge** / **titleMedium** / **titleSmall**: 앱바·카드 제목·섹션·수치
- **bodyLarge** / **bodyMedium** / **bodySmall**: 본문·메타
- **labelLarge** / **labelMedium** / **labelSmall**: 버튼·칩·등락

`SapiensTextStyles`는 브리핑·뉴스·마켓에서 위 역할을 국소적으로 덮는 슬롯이다.

# Sapiens 컬러 · 타이포 가이드 (적용 기준)

앱은 **Material 3 `MaterialTheme`** + **Sapiens 전용 토큰**(`Background`, `TextPrimary` 등)을 함께 씁니다.  
다크/라이트는 `applyThemePalette(darkTheme)`로 **런타임 토큰**이 바뀌고, `SapiensTheme`의 `colorScheme`은 정적 라이트·다크 팔레트와 맞춰져 있습니다.

---

## 1. 공통 컬러 (역할 기준)

| 토큰 | 역할 | 다크(참고) | 라이트(참고) |
|------|------|------------|--------------|
| **Primary** (= **Accent**) | 브랜드 주황, 탭·토글 선택, 강조 아이콘, 로딩 인디케이터 | `#F56E0F` | 동일 |
| **OnPrimaryFixed** | Primary 위 대비용 텍스트·아이콘(예: 스위치 썸) | `#FFFFFF` | 동일 |
| **Background** | 화면 전체 배경 | `#151419` | `#FAFAF8` |
| **Card** | 카드·리스트 서피스, 뉴스 리스트 배경, 마이 카드 등 | `#1B1B1E` | `#FFFFFF` |
| **Elevated** | 한 단계 올린 서피스(시트·모달 등에서 보조) | `#262626` | `#F2F1EE` |
| **TextPrimary** | 본문·제목 1차 | `#FBFBFB` | `#1A1A1A` |
| **TextSecondary** | 부가 설명·캡션·구분선에 가까운 요소 | `#878787` | `#6B6B6B` |
| **TextTertiary** | 힌트·3차(현재는 Secondary와 유사 계열) | — | — |
| **Hair** | 아웃라인·경계선 계열(Material `outline`) | 흰 알파 | 검정 알파 |
| **MarketUp** / **MarketDown** | 등락·지수 상승/하락 색 | 빨강/파랑 계열 | 약간 다른 톤 |
| **MarketFlat** | 보합 등 (고정 회색) | `#878787` | 동일 |
| **SurfaceMuted** | 어두운 중첩 카드·지표 박스 배경 | `#28282A` | 테마와 무관하게 고정에 가깝게 사용되는 경우 있음 |
| **Success / Warning / Error** (+ Container) | 시맨틱 상태(필요 화면만) | 고정 팔레트 | 동일 |

**알파·보조**

- **`ContentAlpha`**: `iconGhost`(닫기 원형 배경 등), `hairlineOnSecondary`(얇은 구분선), `modalScrim` 등.
- **`DividerOnMutedSurface`**: 어두운 카드 위 구분선.
- **`CategoryChipPalette`**: **뉴스** 피드 카테고리 칩 배경/글자색(경제·IT·정치 등).
- **`SectorChipPalette`**: **마켓** 테마/섹터 칩.

---

## 2. 공통 타이포 (역할 기준)

### 2.1 Material `Typography` (`MaterialTheme.typography`)

폰트 패밀리는 현재 **시스템 SansSerif**(`Typography.kt` 주석 참고). 스크린에서 아래 역할을 자주 씁니다.

| 스타일 | 용도(공통 패턴) |
|--------|-----------------|
| **titleLarge** | 큰 제목(예: 종목 상세 헤더 종목명) |
| **titleMedium** | 수치 강조(예: 지표 카드 값, 재무 숫자) |
| **titleSmall** | 섹션 소제목, 리스트 헤드라인(뉴스 제목 등) |
| **bodyLarge** | 본문 강조, 에러 메시지 |
| **bodyMedium** | 일반 본문, 빈 상태 안내 |
| **bodySmall** | 보조 한 줄(거래소 라벨, 캡션) |
| **labelSmall** | 시간·메타, 작은 라벨 |

### 2.2 `SapiensTextStyles` (브리핑·뉴스·마켓에서 보강)

| 토큰 | 용도 |
|------|------|
| **briefingPublisherChip** | 브리핑 국내 카드 상단 언론사 칩 |
| **briefingThemeHeadline** | 브리핑 국내 카드 메인 헤드라인(크게) |
| **morningCardHeadline** / **morningListRow** | 아침/모닝 카드형 리스트(있는 화면) |
| **marketIndexGroup** / **statCaption9** | 작은 그룹 라벨·캡션(9sp 계열, 마켓·종목 상세 지표 라벨 등) |
| **toggleLabel12** | **뉴스** 상단 국내/해외 토글 라벨 |

---

## 3. 메뉴별 적용 요약

### 브리핑

- **배경**: `Background`.
- **섹션 제목**: `SectionLabel` → `MaterialTheme.typography.titleSmall` + **TextPrimary**; 부제 있을 때만 보조색.
- **국내 주요뉴스 카드**: 카드 배경 **Card**; 헤드라인 **SapiensTextStyles.briefingThemeHeadline** + **TextPrimary**; 언론사 칩 **SapiensTextStyles.briefingPublisherChip**; 리스트 **bodyLarge** 등 + **TextPrimary** / 구분선 **TextSecondary** 알파.
- **해외 주요뉴스 카드**: 동일하게 **Card** / **TextPrimary**·**TextSecondary** 계열.

### 뉴스

- **화면 배경**: **Background**; **리스트 카드 영역**: **Card**.
- **상단 탭**: `PrimaryTabRow` — 선택 **TextPrimary**, 비선택 **TextSecondary**; 강조색은 **Accent**(= Primary).
- **국내/해외 토글**: 선택 배경 **Accent**, 라벨 **OnPrimaryFixed** / 비선택 **TextSecondary**; 라벨 타이포 **SapiensTextStyles.toggleLabel12**.
- **뉴스 한 줄(NewsFeedRow)**: 카테고리 칩은 **CategoryChipPalette**; 시간 **labelSmall** + **TextSecondary**; 제목 **titleSmall** + **TextPrimary**; 언론사 **bodySmall** + **TextSecondary**. (해외 탭은 시간·언론사 문자열만 별도 포맷 적용.)

### 마켓

- **목록·탭**: 배경 **Background**; 탭·강조 **Accent**; 본문 계열 **TextPrimary** / **TextSecondary**.
- **섹터 칩**: **SectorChipPalette**.
- **종목 상세(풀스크린 다이얼로그)**: 바깥 **Background**; 구분선 **TextSecondary** + `ContentAlpha.hairlineOnSecondary`; 헤더 종목명 **titleLarge** + **TextPrimary**; 거래소 **bodySmall** + **TextSecondary**; 닫기 버튼 배경 **TextSecondary**·`iconGhost`, 아이콘 **TextPrimary**; 로딩 **Accent**; 투자지표·재무 작은 라벨 **SapiensTextStyles.marketIndexGroup** + **TextSecondary**, 수치 **titleMedium** + **TextPrimary**; 리포트/뉴스 리스트 카드 **Card**, 메타 **labelSmall**·제목 **titleSmall** 앞서와 동일 패턴.

### 마이

- **배경**: **Background**; 카드형 행 **Card**.
- **계정/로그아웃**: **TextPrimary** / **TextSecondary**.
- **푸시 알림 행**: 아이콘 **Primary** 틴트; 제목 **bodyMedium** + **TextPrimary**; 부제 **bodySmall** + **TextSecondary**; 스위치 켜짐 시 **Primary** / **OnPrimaryFixed**.
- **펼침 섹션(저장 기사, API 상태 등)**: 동일하게 **TextPrimary**·**TextSecondary**·**Card** 리듬.

### 공통 UI (여러 메뉴에서 열림)

- **기사 바텀시트**: 시트 배경·본문은 **Card** / **TextPrimary**·**TextSecondary**와 Material 타이포 조합.
- **로딩 인디케이터**: **Accent**.
- **에러/빈 안내 문구**: 보통 **bodyLarge** 또는 **bodyMedium** + **TextSecondary**.

### 시스템 UI

- **알림(푸시)**: 소형 아이콘은 브랜드 벡터 리소스(상태바에서는 시스템이 단색 틴트 처리).

---

## 4. 한눈에: 텍스트 색 규칙

| 색 | 쓰는 곳의 느낌 |
|----|----------------|
| **TextPrimary** | 읽어야 할 제목·본문 |
| **TextSecondary** | 날짜, 부가설명, 비선택 탭, 구분선·아이콘 고스트 |
| **Primary / Accent** | “눌러도 되는 강조”, 진행, 선택 |
| **OnPrimaryFixed** | 주황 배경 위 글자/아이콘 |
| **MarketUp / MarketDown** | 숫자 등락 방향만 (가격 UI는 화면별로 노출 여부 다름) |

---

## 5. 참고

- **다크/라이트 전환** 시 실제로 바뀌는 것은 주로 **Background, Card, Elevated, Text\*, Hair, MarketUp/Down** 런타임 토큰입니다.
- **CategoryChipPalette / SectorChipPalette / SurfaceMuted** 등은 **테마 전환과 독립적인 고정 색**에 가깝게 쓰이는 경우가 많습니다.
- 상세 수치·간격 토큰은 `Spacing`, `Dimens`(카드 패딩, 시트 여백 등)와 함께 쓰이며, **색·타이포 가이드와 별도**로 레이아웃 리듬을 맞추는 용도입니다.

# Figma: 브리핑 화면 360×780 (다크) 빌드 가이드

대상 파일: [sapiens (Figma)](https://www.figma.com/design/EaE3QXT7UV66RFZRXppOvi/sapiens)

이 문서는 **에이전트가 Figma 캔버스에 직접 그릴 수 없을 때** 디자이너가 동일 결과를 만들기 위한 스펙입니다.  
변수·텍스트 스타일 이름은 **파일에 이미 등록된 것**을 기준으로 하되, `figma-setup.ps1`으로 올린 컬렉션은 `Sapiens Colors` / 모드 **Dark**이고, 토큰 JSON(`sapiens-tokens (1).json`)과 앱 가이드(`color-type-guide.md`)와 맞춥니다.

**모드:** Variables 패널에서 **Dark** 활성화 후 작업합니다.

---

## 1. 프레임

| 항목 | 값 |
|------|-----|
| 이름 | `Briefing / Mobile / 360×780` (임의) |
| 크기 | **360** × **780** |
| 채우기 | 변수 **`Background`** (UI/문서에서 말하는 *Background/Default*와 동일 역할) |

---

## 2. 레이어 트리 (권장)

```
Briefing / Mobile / 360×780  [Frame, fill = Background]
├── AppBar                    [Auto layout, H, 360×56, pad 20/16]
│   └── Title "브리핑"         [Text style: 아래 §4 참고]
├── Scroll (optional)         [Frame 또는 Auto layout vertical]
│   ├── Section1              [Vertical, gap 12, pad H 20]
│   │   ├── LabelRow          [국내 주요뉴스 + 부제]
│   │   ├── CardDomestic_A
│   │   └── CardDomestic_B
│   ├── Section2              [Vertical, gap 12, pad H 20, top 24]
│   │   ├── LabelRow
│   │   ├── CardOverseas_A
│   │   └── CardOverseas_B
│   └── TokenStrip (선택)     [§3.2 — 파일 내 색 변수 전부 노출용]
```

---

## 3. 색상 변수 적용

### 3.1 브리핑 UI 본연

| 영역 | 변수 |
|------|------|
| 화면 배경 | **Background** |
| 앱바 배경 | **Background** 또는 **Elevated** (한 단만 올리고 싶을 때) |
| 국내·해외 카드 `Surface` | **Card** |
| 카드 테두리(선택) | **Hair** (1px stroke) |
| 섹션 제목 | **TextPrimary** |
| 섹션 부제·메타·구분선 느낌 | **TextSecondary** (필요 시 opacity) |
| 포인트(칩 강조·뱃지) | **Accent** |
| “보합·중립” 등 메타 숫자(예시) | **MarketFlat** |
| 상승/하락 예시 텍스트(선택 한 줄) | **MarketUp** / **MarketDown** |
| 카드 안 작은 박스(지표 느낌) | **SurfaceMuted** |
| 성공/경고/오류 뱃지(아주 작게) | **Success** / **Warning** / **Error** |

### 3.2 “파일에 등록된 색 변수 전부” 쓰기 (`figma-setup.ps1` 기준 13개)

위 표에 이미 **Background, Card, Elevated, TextPrimary, TextSecondary, Hair, Accent, MarketUp, MarketDown, MarketFlat, SurfaceMuted, Success, Warning, Error** 가 포함됩니다.  
본문 카드만으로 안 쓰인 변수가 있으면, 프레임 하단에 **12×12 스와치 행**을 하나 두고 각 사각형 fill에 변수만 연결해 두면 “전부 활용” 조건을 만족시키면서 UI는 깔끔하게 유지할 수 있습니다.

> Figma 파일에 **JSON 토큰의 `category_*` / `surfaceChartInactive` / `onPrimaryFixed`** 등이 별도 변수로 더 있다면, 같은 방식으로 스와치 행에 추가하거나 칩 배경에 연결하면 됩니다.

---

## 4. 텍스트 스타일 전부 활용 (`sapiens-tokens (1).json` 기준)

Figma **Text styles** 이름이 JSON·앱과 다를 수 있으므로, 아래 **역할 → 권장 배치**로 스타일을 한 번씩 적용합니다.

| 텍스트 스타일 (토큰 JSON 키) | 이 프레임에서의 사용 예 |
|------------------------------|-------------------------|
| **displayLarge** | (선택) 프레임 외곽 워터마크/섹션 데코용 한 줄 — 본 UI에서는 생략 가능; “전부” 조건이면 아주 작은 영역에 한 글자 |
| **headlineMedium** | 해외 카드 A의 **영문 헤드라인** 대용 |
| **headlineSmall** | 해외 카드 B의 **영문 헤드라인** 대용 |
| **titleLarge** | 앱바 **"브리핑"** (앱은 `titleLarge`+6sp이나 Figma는 `titleLarge`만 연결해도 됨) |
| **titleMedium** | 국내 카드 내 **가짜 지표 숫자** 한 줄 |
| **titleSmall** | 섹션 제목 **「국내 주요뉴스」**, **「해외 주요뉴스」** |
| **bodyLarge** | 국내 카드 **리스트 헤드라인** 첫 줄 |
| **bodyMedium** | 국내 카드 **리스트 부연** 또는 해외 카드 본문 |
| **bodySmall** | 시간·언론사 **「09:30 · 매일경제」** |
| **labelLarge** | 카드 하단 **「원문 보기」** 버튼 라벨 |
| **labelMedium** | 보조 태그 한 줄 |
| **labelSmall** | 칩 옆 **순위·뱃지** |
| **briefingPublisherChip** | 국내 카드 상단 **「한국경제」** 칩 |
| **briefingThemeHeadline** | 국내 카드 **메인 헤드라인** (큰 제목) |
| **morningCardHeadline** | 국내 카드 B 상단 **짧은 모닝형 제목** |
| **marketIndexGroup** | **SurfaceMuted** 박스 안 라벨 (예: `KOSPI · 지수`) |
| **toggleLabel12** | 앱바 아래 **「국내 피드」** 같은 보조 캡슐(선택) — 뉴스 토글과 동일 스타일 재사용 |

---

## 5. 카드 콘텐츠 (복붙용 더미)

**국내 카드 A**

- 칩: `한국경제` — 스타일 **briefingPublisherChip**, 색 **Accent** 위 **OnPrimaryFixed**가 아니라 칩은 배경 **SurfaceMuted** + 글자 **TextPrimary** 또는 브랜드에 맞게 **Accent** 배경 + 흰 글자(변수 **OnPrimaryFixed**가 파일에 있으면 글자에 연결).
- 헤드라인: `금리 인하 기대감에 코스피 사흘 연속 상승` — **briefingThemeHeadline**, **TextPrimary**
- 리스트 1행: `외국인 순매수 전환…` — **bodyLarge**, **TextPrimary**
- 메타: `09:12 · 매일경제` — **bodySmall**, **TextSecondary**

**국내 카드 B**

- **morningCardHeadline**: `오늘의 증시 체크`
- **bodyMedium** 본문 한 줄
- **labelSmall** + **MarketUp** 예: `+1.2%`

**해외 카드 A**

- **headlineMedium** + **TextPrimary**: `Wall St edges higher as data cools`
- **bodyMedium** + **TextSecondary**: 요약 2줄

**해외 카드 B**

- **headlineSmall** + **TextPrimary**
- **bodySmall** 메타

카드 컨테이너: 모서리 앱과 맞추려면 **12px** 라운드 권장 (`AppShapes` 계열).

---

## 6. 스페이싱 (앱과 근접)

- 화면 좌우 패딩 **20**
- 섹션 라벨 상하 **12**
- 카드 내부 패딩 **16** (상하 약간 여유 시 **20**)
- 카드 간 **12**

---

## 7. 자동화에 대해

Figma **REST API**는 주로 파일 메타·노드 **읽기**에 쓰이고, 복잡한 프레임 생성은 **Plugin**이나 수작업이 일반적입니다.  
저장소의 `figma-setup.ps1`은 **Variables 생성**용이며, **FIGMA_TOKEN**을 환경변수로만 두는 것을 권장합니다(토큰은 커밋하지 않기).

---

## 8. 체크리스트

- [ ] 프레임 360×780, fill = **Background**, 모드 **Dark**
- [ ] 앱바 타이틀 **브리핑** + §4 스타일 분배
- [ ] 국내 섹션 + 카드 2
- [ ] 해외 섹션 + 카드 2
- [ ] `figma-setup` 기준 색 변수 13개 전부 레이어에 연결(카드 + 하단 스와치)
- [ ] §4 텍스트 스타일 전부 한 번 이상 적용

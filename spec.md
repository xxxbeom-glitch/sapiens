d:\project\dailynews\spec.md
# 브리핑 (DailyNews) — 프로젝트 스펙 문서

> 한국 사용자를 위한 AI 뉴스 & 마켓 브리핑 모바일 UI 프로토타입
> 최종 업데이트: 2026-04-18

---

## 1. 프로젝트 개요

### 1.1 한 줄 요약
아침마다 한국·미국 주요 뉴스와 시장 지표를 한 화면에 요약해서 보여주는 모바일 앱의 **정적 UI 프로토타입** (프론트엔드 단독, 모의 데이터).

### 1.2 제품 콘셉트
- **타겟**: 출근길/아침에 뉴스와 시장 상황을 빠르게 훑고 싶은 국내 투자자·직장인
- **핵심 가치**: "AI 요약 · 06:00 업데이트" — 매일 아침 6시에 자동 요약된 브리핑 한 화면
- **디자인 언어**: Material Design 3 기반 다크 테마 + Liquid Lava(`#F56E0F`) 포인트 컬러
- **한국어 UX 규칙**: 상승(빨강) / 하락(파랑) / 보합(회색) — 국내 증시 색 컨벤션 준수

### 1.3 현재 상태
- UI 구현체만 존재하는 **정적 프로토타입** (백엔드·빌드 파이프라인 없음)
- `Briefing.html`을 브라우저에서 직접 열거나 간이 로컬 서버로 열면 즉시 실행됨
- 모든 뉴스/지표/종목 데이터는 `data.jsx`에 하드코딩된 **목(mock) 데이터**
- Git 저장소는 아직 초기화되어 있지 않음 (터미널 환경 warming 표시)
- `package.json`, 빌드 스크립트, 테스트, 린터 설정 없음

---

## 2. 기술 스택

### 2.1 런타임 / 프레임워크
| 영역 | 선택 | 버전 | 로딩 방식 |
|---|---|---|---|
| UI 라이브러리 | React | `18.3.1` | unpkg CDN (`react.development.js`) |
| 렌더러 | ReactDOM | `18.3.1` | unpkg CDN |
| JSX 변환 | @babel/standalone | `7.29.0` | 브라우저 in-line 변환 (`<script type="text/babel">`) |
| 모듈 시스템 | 없음 (전역 `window` 공유) | — | `Object.assign(window, …)` 패턴 |

> 빌드 단계가 없다 → **모든 JSX는 런타임에 브라우저에서 트랜스파일**됩니다. 프로덕션용이 아니라 디자인 검토용 프로토타입 구성입니다.

### 2.2 스타일링
- **순수 인라인 스타일** (CSS-in-JS 라이브러리 없음)
- CSS 변수(`--md-*`)를 `Briefing.html`의 `:root`에 정의 → Material 3 토큰 매핑
- JS 측에서는 `components.jsx`의 `M3` / `C` / `M3_TYPE` 상수로 재표현
- 호버/프레스 인터랙션은 `.press` 유틸리티 클래스로 처리

### 2.3 타이포그래피
- **본문(한글)**: `SUIT` — 로컬 `fonts/` 디렉터리에 4 weight 수록
  - `suit_regular.ttf` (400)
  - `suit_medium.ttf` (500) — 기본 body weight
  - `suit_semibold.ttf` (600)
  - `suit_bold.ttf` (700)
- **숫자/영문 모노**: `Roboto Mono` 400/500 — Google Fonts CDN
- 시스템 폴백: `system-ui, sans-serif`
- `font-feature-settings: "tnum" 1` — 숫자 고정폭(tabular numbers) 활성화

### 2.4 에셋
- `fonts/` — 프로덕션에서 사용되는 SUIT TTF 4종
- `uploads/` — `fonts/`와 동일한 TTF 4종 사본 (임시 업로드 버퍼로 추정, 중복)
- `screenshots/` — 디자인 비교용 캡처 6장
  - `main.png`, `android-main.png`, `dark-briefing.png`, `ios-briefing.png`, `ios-style.png`, `suit.png`

---

## 3. 디렉터리 / 파일 구조

```
d:\project\dailynews\
├── Briefing.html         # 엔트리 포인트 (HTML 셸 + CSS 토큰 + CDN 로딩 + Tweaks 패널)
├── android-frame.jsx     # Android Material 3 디바이스 프레임/상태바/네비바/키보드 컴포넌트
├── data.jsx              # 모든 모의 데이터 (뉴스/지표/종목/기사)
├── components.jsx        # 브리핑 앱의 UI 컴포넌트 (MorningCard, USMarketCard, BottomSheet 등)
├── app.jsx               # 최상위 App 컴포넌트 — 탭 라우팅, 화면 조립, ReactDOM.createRoot
├── fonts/                # SUIT 한글 TTF 4 weight
├── uploads/              # fonts/ 사본 (중복 에셋)
├── screenshots/          # 레퍼런스/결과 스크린샷
└── spec.md               # 본 문서
```

### 3.1 파일별 책임

#### `Briefing.html`
- `<html lang="ko">` 선언 + 뷰포트 설정
- `@font-face` 로 `fonts/suit_*.ttf` 4종 등록
- CSS 변수로 Material 3 다크 팔레트(Liquid Lava accent) 정의
- `--up / --down / --flat` — 한국식 시장 색 상수
- `.stage` — 폰 프레임을 중앙 배치하는 스테이지(라디얼 그라디언트 배경)
- `#tweaks` — 우측 하단 플로팅 디자인 Tweaks 패널
- React / ReactDOM / Babel standalone CDN `<script>` 삽입 (SRI 해시 포함)
- `window.__TWEAK_DEFAULTS` — 초기 디자인 토글 값(`density`, `accent`, `showIndicators`, `cardStyle`)
- 하단 `<script>`에서 Tweaks UI ↔ `window.__tweaks` 상태 연결, `tweakschange` 커스텀 이벤트 브로드캐스트
- `window.parent.postMessage({ type: '__edit_mode_*' })` — Figma-like 편집 환경 메시지 핸드셰이크(부모 프레임과의 에디트 모드 연동)

#### `data.jsx`
- `MORNING_ARTICLES` — 한국 조간 카드 4건 (한국경제/매일경제/조선비즈/연합뉴스)
- `MARKET_INDICATORS` — 다우/나스닥/S&P/달러원/금/WTI 6개
- `US_REPORT` — 미국 시황 리포트 본문 1개
- `US_ARTICLES` — 블룸버그/로이터/WSJ/CNBC/FT 등 6건
- `WATCHLIST` — 삼성전자/SK하이닉스/NVDA/NAVER/AAPL/LG화학/TSLA 7종
- `NEWS_FEED` — 카테고리별 단신 6건
- 모든 배열을 `Object.assign(window, …)` 로 전역 노출

#### `components.jsx`
- `M3`, `C`, `M3_TYPE` — Material 3 토큰/색상/타이포 상수
- `useTweaks()` — 전역 Tweaks 상태를 구독하는 React 훅 (커스텀 이벤트 기반)
- `densityPad(density)`, `cardShell(style)` — 디자인 토글에 반응하는 유틸
- `<Icon name="…" />` — 인라인 SVG 아이콘 세트 (chevron/arrow/search/bell/feed/article/trending/bookmark/open-in-new/more-vert)
- `<TopAppBar>` — 제목 + 오늘 날짜 + 메뉴(glass blur backdrop)
- `<SectionLabel>` — 대문자 섹션 헤더 + 부제
- `<SourceChip>` — Liquid Lava tint 언론사 칩
- `<MorningCard>` — 좌우 스와이프 + 스크롤 스냅 + 페이지 인디케이터
- `<USMarketCard>` — 시장 지표 테이블 + AI 시황 리포트 + 주요 기사 리스트 복합 카드
- `<BottomSheet>` — 기사 상세 바텀시트 (저장 / 원문 보기 CTA 2개)
- `<NavBar>` — 3탭 하단 내비게이션(브리핑/뉴스/관심종목), glass blur

#### `app.jsx`
- `NewsScreen` — `NEWS_FEED` 리스트 렌더
- `WatchScreen` — `WATCHLIST` 그리드 렌더, 등락률 색상 분기
- `BriefingScreen` — MorningCard + USMarketCard 조립 + 푸터 문구
- `App` —
  - `tab` 상태를 `localStorage['briefing:tab']`에 영속화
  - `412 × 892` 픽셀 픽셀3a/Pixel 사이즈 디바이스 프레임 렌더
  - 상단 펀치홀 + `AndroidStatusBar` + `TopAppBar` + 콘텐츠 영역 + `NavBar` + `BottomSheet` 오버레이
  - `ReactDOM.createRoot(document.getElementById('root')).render(<App />)`

#### `android-frame.jsx`
- `AndroidStatusBar`, `AndroidAppBar`, `AndroidListItem`, `AndroidNavBar`, `AndroidKeyboard`, `AndroidDevice` 래퍼
- 현재 `App`은 `AndroidStatusBar`만 사용하지만, 나머지는 다른 데모 레이아웃용으로 보존됨

---

## 4. 화면 / 기능 명세

### 4.1 공통 셸
- 디바이스 프레임: `412 × 892`, 라운드 `48px`, 10px 무광 블랙 테두리 + 카메라 펀치홀
- 상단 `AndroidStatusBar` (9:30 시각 고정, 다크 아이콘)
- 중앙 `TopAppBar` — 탭 제목 + 우측에 `4월 18일 (토)` 모노 표기
- 하단 3탭 `NavBar` — `브리핑 / 뉴스 / 관심종목`
- 모든 리스트 아이템은 `.press` 호버/프레스 피드백

### 4.2 브리핑 탭 (`briefing`)
1. **아침 브리핑 섹션**
   - 가로 스와이프 카드 4장 (한국 조간 요약)
   - 각 카드: `SourceChip` + 태그(ECON/MARKET/…) + 2줄 헤드라인 + 2줄 요약 + 출처·시간
   - Tweaks에서 `showIndicators` ON이면 페이지 인디케이터 표시
2. **미국 장 시황 섹션** — 복합 카드 1개
   - **시장 지표 테이블**: 다우/나스닥/S&P/달러원/금/WTI (한국식 등락 색)
   - **미국 시황 리포트**: AI 작성 본문 (엔비디아 3.2% ↑ 등)
   - **주요 기사 리스트**: 블룸버그/로이터/WSJ 등 6건, 탭 시 바텀시트
3. **푸터**: `AI 요약 · 06:00 업데이트`
4. 기사/카드 탭 → `BottomSheet` 열림 (저장 / 원문 보기 CTA)

### 4.3 뉴스 탭 (`news`)
- 카테고리 배지 + 상대시각 + 헤드라인 + 출처 리스트 (6건)
- 탭 시 바텀시트

### 4.4 관심종목 탭 (`watch`)
- 종목코드 칩 + 종목명 + 현재가(모노) + 등락률(색상) 4열 그리드
- 국내/해외 혼합 (원화 `78,400`, 달러 `$928.14`)

### 4.5 바텀시트
- 반투명 백드롭 + 슬라이드 업 애니메이션 (`sheetIn 0.32s cubic-bezier`)
- 핸들 바 → 칩/시간 → 제목 → 요약 → `저장` / `원문 보기` 2버튼

### 4.6 Tweaks 패널 (디자이너 토글)
- `Density`: compact / comfortable / spacious
- `Accent`: green(KR) / blue / mono — (현재 팔레트는 Liquid Lava 고정, 토글은 준비만)
- `Card style`: soft / outlined / flat
- `Indicators`: MorningCard 인디케이터 표시 여부
- 부모 프레임(예: Figma-like 에디터)과 `postMessage` 로 연동해서 값 영속화 가능

---

## 5. 디자인 토큰

### 5.1 색상 (다크 테마)
| 토큰 | 값 | 용도 |
|---|---|---|
| `--md-surface` / `C.page` | `#151419` (Dark Void) | 페이지 배경 |
| `--md-surface-container` / `C.card` | `#1B1B1E` (Gluon Grey) | 카드 |
| `--md-surface-container-high` / `C.elevated` | `#262626` (Slate Grey) | 칩/인풋 |
| `--md-on-surface` / `C.ink` | `#FBFBFB` (Snow) | 본문 텍스트 |
| `--md-on-surface-var` / `C.mute` | `#878787` (Dusty Grey) | 보조 텍스트 |
| `--md-primary` / `C.accent` | `#F56E0F` (Liquid Lava) | 포인트 |
| `--md-outline-variant` / `C.hair` | `rgba(255,255,255,0.08)` | 헤어라인 구분자 |
| `--up` | `#FF3B30` | 상승(빨강, KR) |
| `--down` | `#0064FF` | 하락(파랑, KR) |
| `--flat` | `#878787` | 보합 |

### 5.2 타이포 스케일 (`M3_TYPE`)
Material 3 type scale 준수 — `displaySmall`·`headlineLarge/Medium/Small`·`titleLarge/Medium/Small`·`bodyLarge/Medium/Small`·`labelLarge/Medium/Small`

### 5.3 인터랙션
- `.press` — `background-color .18s` 트랜지션, hover `rgba(255,255,255,0.03)`, active `rgba(255,255,255,0.06)`
- `sheetIn` — 바텀시트 `translateY(100% → 0)` 0.32s
- `fadeIn` — 백드롭 0.18s

---

## 6. 실행 방법

### 6.1 가장 간단한 방법
```powershell
# 프로젝트 루트에서
start .\Briefing.html
```
대부분의 데스크톱 브라우저에서 즉시 렌더됩니다. 단, `file://` 프로토콜은 일부 브라우저에서 `@font-face` 로딩을 차단할 수 있어 SUIT 한글 폰트가 폴백될 수 있습니다.

### 6.2 로컬 서버로 실행 (권장)
폰트·CDN 모두 문제없이 불러오려면 정적 서버를 띄웁니다.

```powershell
# Python 3
python -m http.server 5500
# 또는 Node 설치되어 있으면
npx serve .
```
브라우저에서 `http://localhost:5500/Briefing.html` 접속.

### 6.3 Tweaks 패널 활성화
- 기본은 숨김. 부모 프레임이 `postMessage({ type: '__activate_edit_mode' }, '*')` 를 보내면 열림.
- 로컬에서 강제로 열려면 DevTools 콘솔에서:
  ```js
  document.getElementById('tweaks').classList.add('on')
  ```

---

## 7. 데이터 모델 (Mock)

```ts
// data.jsx 에 하드코딩
type Article = {
  source: string;          // 예: '한국경제'
  sourceColor?: 'kr';      // 칩 톤 키
  headline: string;
  summary: string;
  time: string;            // '오전 6:42'
  tag: string;             // 'ECON' | 'MARKET' | 'TECH' | ...
};

type MarketIndicator = {
  name: string;            // '다우존스'
  value: string;           // '39,582.10'
  change: string;          // '+0.84%' / '−0.34%'
  dir: 'up' | 'down' | 'flat';
};

type WatchlistItem = {
  ticker: string;          // '005930' | 'NVDA'
  name: string;
  price: string;
  change: string;
  dir: 'up' | 'down' | 'flat';
};

type NewsItem = {
  cat: string;             // '경제' | 'IT' | '정치' | ...
  title: string;
  time: string;            // '12분 전'
  source: string;
};

type USReport = { date: string; body: string };
```

> 실제 서비스화 시 이 형상을 그대로 API 응답 스키마로 채택할 수 있습니다.

---

## 8. 현재까지의 진행 / 의사결정 요약

| 시점 | 상태 |
|---|---|
| 디자인 | Material 3 다크 + Liquid Lava accent + 한국식 등락 색 확정 |
| 화면 | 브리핑 / 뉴스 / 관심종목 3탭 구현 완료 |
| 컴포넌트 | 카드(스와이프), 지표 테이블, 리포트, 바텀시트, 내비바 구현 |
| 디바이스 프레임 | Android Pixel 스타일 `412×892` 채택, iOS 버전은 스크린샷만 존재 |
| 데이터 | 전부 mock, 외부 API·백엔드 없음 |
| 폰트 | SUIT(KR) 로컬 + Roboto Mono(CDN) |
| 빌드 | 없음 — Babel standalone 런타임 변환 |
| 배포 | 없음 — `Briefing.html` 더블클릭 또는 로컬 서버 |

### 알려진 제약 / 개선 여지
- **빌드리스 런타임 변환**: 프로덕션에 부적합. 실서비스 전 Vite/Next.js 등으로 마이그레이션 필요.
- **전역 window 오염**: 모든 모듈이 `window`를 통해 서로를 참조 → ES 모듈로 전환 권장.
- **모의 데이터만 존재**: 실제 뉴스 수집 파이프라인(크롤링/AI 요약/DB) 설계 필요.
- **`fonts/`와 `uploads/` 중복**: 중 하나 정리 필요.
- **Tweaks `accent` 토글**이 실제 팔레트 스와핑에 연결되어 있지 않음(값만 저장됨).
- **접근성**: 탭 인덱스/ARIA 레이블/포커스 링 부재.
- **i18n/다크-라이트 모드 스위치**: 다크 고정.
- **Git 미초기화** — 버전 관리 시작 필요.

---

## 9. 다음 단계 제안 (로드맵 초안)

### Phase 1 — 기초 다지기
- [ ] `git init` + 원격 저장소 연결
- [ ] Vite + React + TypeScript 프로젝트로 이전, JSX → TSX 전환
- [ ] `window` 전역 의존 제거, ES 모듈로 재구성
- [ ] ESLint / Prettier / Husky 세팅

### Phase 2 — 데이터 파이프라인
- [ ] 뉴스 소스 API 계약 정의 (RSS / 네이버뉴스 / 외신)
- [ ] AI 요약 모듈 (OpenAI/Claude/Gemini) + 프롬프트 명세
- [ ] 시세 API (한국투자증권 / 야후 파이낸스 등) 연동
- [ ] 매일 06:00 배치 스케줄러 (Supabase cron / GitHub Actions / Cloud Scheduler)
- [ ] 영속 스토리지 (Supabase / Firebase / PlanetScale)

### Phase 3 — 앱화
- [ ] PWA 메타/매니페스트
- [ ] React Native / Expo 버전 분기 (iOS 스크린샷 기준 이미 디자인 레퍼런스 있음)
- [ ] 푸시 알림 (매일 06:00 "오늘의 브리핑")
- [ ] 사용자 로그인 + 관심종목 커스터마이징 영속화

### Phase 4 — 품질
- [ ] 컴포넌트 단위 테스트 (Vitest + Testing Library)
- [ ] Playwright 스냅샷 테스트로 스크린샷 회귀 검증
- [ ] Lighthouse 접근성 점수 90+
- [ ] 다국어(KR/EN) 지원

---

## 10. 빠른 참조

| 찾고 싶은 것 | 위치 |
|---|---|
| 색상 토큰 | `Briefing.html` `:root`, `components.jsx` `C` |
| 타이포 토큰 | `components.jsx` `M3_TYPE` |
| 모의 데이터 수정 | `data.jsx` |
| 상단 앱바 | `components.jsx` `TopAppBar` |
| 탭 상태 영속화 | `app.jsx` `localStorage['briefing:tab']` |
| Tweaks 기본값 | `Briefing.html` `window.__TWEAK_DEFAULTS` |
| 디바이스 프레임 크기 | `app.jsx` App 컴포넌트 루트 `div` (`412 × 892`) |
| 아이콘 추가 | `components.jsx` `Icon` 컴포넌트 `switch` |

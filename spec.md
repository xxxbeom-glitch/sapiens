# Sapiens — 제품·기술 스펙 (spec)

본 문서는 저장소 **현재 적용 상태**를 기준으로 앱 이름, 패키징, 아키텍처, 데이터 수집·파싱, AI, 디자인 규격, 주요 컴포넌트를 한곳에 정리합니다. 세부 토큰 수치·화면별 색 규칙은 `design system.md`, `color-type-guide.md`를 참고하세요.

---

## 1. 제품 정체성

| 항목 | 값 |
|------|-----|
| **앱 이름** | Sapiens (`AndroidManifest` / `strings.xml` `app_name`) |
| **한 줄 설명** | 한국 투자자 대상 금융·시장 뉴스·브리핑·마켓 정보를 제공하는 Android 앱 |
| **루트 프로젝트명** | `Sapiens` (`settings.gradle.kts`) |

---

## 2. 앱 패키징·빌드

| 항목 | 값 |
|------|-----|
| **applicationId** | `com.sapiens.app` |
| **namespace** | `com.sapiens.app` |
| **minSdk** | 26 |
| **targetSdk** | 36 |
| **compileSdk** | 36 (minor 1) |
| **versionCode / versionName** | 1 / `"1.0"` |
| **Java** | 11 (source/target compatibility) |
| **UI 프레임워크** | Jetpack Compose + Material 3 (`material3` 1.3.1 엄격 고정) |
| **주요 스택** | Kotlin, Coroutines, ViewModel, DataStore, WorkManager, Retrofit/OkHttp, Coil, Firebase (Firestore, Auth, Messaging), Google Sign-In |

### 로컬·빌드 설정

- **공공데이터 API**: `local.properties`의 `PUBLIC_DATA_API_KEY` → `BuildConfig.PUBLIC_DATA_API_KEY` (종목 상세 등).
- **Firebase 클라이언트**: `app/google-services.json`은 gitignore 대상. CI는 `tools/google-services.ci.json` 복사로 검증 (`docs/app-guardrails-ko.md`).
- **검증 태스크**: `./gradlew :app:verifyApp` → `lintDebug` (푸시 전 권장).

---

## 3. 저장소 구조 (모듈)

| 경로 | 역할 |
|------|------|
| **`app/`** | Android 앱 단일 모듈. UI·데이터·동기화 전부 여기 |
| **`pipeline/`** | Python 배치: 크롤링 → LLM 요약/분석 → Firestore 쓰기 |
| **`docs/`** | 운영·가이드 (예: `app-guardrails-ko.md`) |
| **`scripts/`** | 로컬/CI 검증 스크립트 등 |
| **`.github/workflows/`** | PR 시 Android 검증 등 |

---

## 4. 앱 아키텍처 개요

### 4.1 진입점

- **`MainActivity`**: Compose 진입.
- **`SapiensApplication`**: 알림 채널 `sapiens_news` 생성.
- **`SapiensFirebaseMessagingService`**: FCM 수신.

### 4.2 내비게이션 (하단 탭)

`MainScreen.kt` 기준 4탭:

| 탭 | 화면 | ViewModel |
|----|------|-----------|
| 브리핑 | `BriefingScreen` | `BriefingViewModel` |
| 뉴스 | `NewsScreen` | `NewsViewModel` |
| 마켓 | `MarketScreen` | `MarketViewModel` |
| 마이 | `MyScreen` | `AuthViewModel` 등 |

종목 상세는 `StockDetailBottomSheet` + `StockDetailViewModel` (`StockDetailRepositoryImpl`).

### 4.3 데이터 레이어 (요약)

- **`NewsRepository` / `NewsRepositoryImpl`**: Firestore 실시간 스냅샷(Flow) + 지수는 Daum/Yahoo HTTP 직접 호출 후 로컬 캐시.
- **`StockDetailRepositoryImpl`**: 네이버 금융·공공데이터 등 (Retrofit/OkHttp).
- **`IndustryRepository`**, **`ThemeDescriptionRepository`**: 마켓 보조 데이터.
- **`ArticleBookmarksRepository`** + **`SavedArticlesFirestoreWriter`**: 저장 기사.
- **`UserCloudBackupRepository` / `UserCloudBackupScheduler`**: 로그인 사용자 클라우드 백업 스케줄.
- **`FeedbackRepositoryImpl`**: 피드백 기록.

---

## 5. Firestore · 데이터베이스

### 5.1 데이터베이스 ID

- 앱·파이프라인 모두 **논리 DB ID**: `sapiens`  
  - 앱: `FirebaseFirestore.getInstance(..., "sapiens")`  
  - 파이프라인: 환경변수 `DATABASE_ID` (기본 `sapiens`)

### 5.2 컬렉션·문서 맵 (앱이 읽는 주요 경로)

| 경로 | 내용 |
|------|------|
| **`briefing/hankyung`** | `articles` 배열 — 한국경제 신문 브리핑 풀(파이프라인 상한 5건 등) |
| **`briefing/maeil`** | `articles` 배열 — 매일경제 신문 브리핑 풀 |
| **`briefing/us_market`** | `articles` 배열 — 해외 RSS 풀 큐레이션 후 요약된 미국 시장 뉴스 |
| **`market/indicators`** | `indicators` 배열 — 시장 지표 |
| **`market/themes/by_no/{no}`** | 테마별 종목·등락률·설명 등 (파이프라인 `rank` 정렬) |
| **`news/realtime`**, **`popular`**, **`main`** | 국내 뉴스 피드 `articles` |
| **`news/overseas_stocks`**, **`news/overseas_tech`** | 해외 뉴스 피드 `articles` |
| **`settings/ai_config`** | `selected_model`: `gemini` \| `claude` (파이프라인·앱 DataStore와 동기 개념) |
| **`saved_articles/{article_id}`** | 사용자 저장 기사 (파이프라인 피드 삭제 시 보호 ID) |
| **`pipeline_logs/{...}`** | 파이프라인 실행 로그·토큰 사용량 등 (`firebase_client.write_pipeline_log`) |

### 5.3 앱 모델 `Article` ↔ Firestore 필드

앱은 `FirestoreMappers`로 맵을 `Article`로 변환합니다.

| Article 필드 | Firestore (우선순위) |
|--------------|---------------------|
| `headline` | `headline_ko` → `headline` |
| `summary` | `summary` |
| `time` | `time` |
| `category`, `tag` | `category` 등 |
| `summaryPoints` | `summaryPoints` |
| `imageUrl` / `thumbnailUrl` | `imageUrl`, `image_url`, `thumbnailUrl`, `thumbnail_url` 등 별칭 |
| `url` | `url`, `link` 등 |

**안정 ID**: 파이프라인 `id`가 없을 때 `Article.stableId()` = SHA-256(`source|time|headline`) (`ArticleStableId.kt`).

### 5.4 파이프라인이 쓰는 저장 스키마 (요약)

- 뉴스·브리핑 피드는 단일 문서에 **`articles` 배열** + **`updated_at`** 형태로 덮어쓰기(`merge=False` 등 문서별).
- `summarizer.merge_to_firestore_article` 출력 키 예: `source`, `headline`, `headline_ko`, `headline_en`, `imageUrl`, `summary`(최대 500자), `time`, `category`, `summaryPoints`, `url` 등.

---

## 6. 데이터 수집·파싱 (파이프라인)

실행 엔트리: **`pipeline/main.py`** 의 `run()`.  
환경: **`pipeline/.env`** (`dotenv`).  
섹션 분할: 환경변수 **`PIPELINE_SECTION`**: `all`(기본) | `briefing` | `domestic_news` | `overseas_news` | `market` | `full`.

### 6.1 휴장·스킵 규칙 (`is_skip_day`)

- **일요일**: `briefing`, `domestic_news`, `market`, `overseas_news` 등 섹션별 스킵.
- **한국 공휴일**: `briefing`, `market` 스킵.
- **NYSE 휴장일**: `briefing`, `overseas_news` 스킵.

### 6.2 크롤러 (`pipeline/crawler.py`) — 방식 요약

| 영역 | 방식 | 비고 |
|------|------|------|
| **국내 뉴스** | 네이버 금융 등 HTML + **BeautifulSoup(lxml)** | 기사 본문은 URL별 셀렉터·노이즈 제거(`_element_to_plain_article_text`), 본문 상한 **`ARTICLE_BODY_MAX_CHARS` (2000)** |
| **해외 뉴스(탭)** | **RSS** (`feedparser`) — CNBC, Guardian, Verge, Ars 등 | 항목별 `summary`/`description`의 HTML을 BS로 텍스트화, **`SUMMARY_CHARS` (200)** 자름 |
| **브리핑 해외** | RSS 풀 → **`curate_us_market_articles`** (LLM으로 8개 선별) → 기사별 요약 | |
| **브리핑 국내 신문** | 한경·매경 전용 크롤 + 본문 필터(`BRIEFING_NEWSPAPER_MIN_BODY_CHARS` 200) | 풀에서 상위 5건씩, `briefing_newspaper=True` 요약 |
| **마켓 테마·업종** | 네이버 증권 페이지 파싱 | `save_market_themes` / `save_market_industries` |
| **시장 지표** | 크롤링된 지표 배열 | 브리핑 화면 등 |
| **종목 번들 (`full` 파이프라인)** | Yahoo 등 재무·애널리스트 데이터 수집 후 **`analyze_company`** | 종목 문서에 AI 분석 필드 merge |

추가: **토스증권** 뉴스는 Playwright 기반 함수가 코드에 보존되어 있으나, 주석상 국내 크롤 경로에서는 미사용일 수 있음.

### 6.3 Python 의존성 (`pipeline/requirements.txt`)

`requests`, `beautifulsoup4`, `lxml`, `feedparser`, `anthropic`, `google-genai`, `firebase-admin`, `playwright`, `pytz`, `holidays`, `pandas-market-calendars` 등.

### 6.4 Firebase 서버 자격 (파이프라인)

우선순위: **`FIREBASE_SERVICE_ACCOUNT_B64`** → **`FIREBASE_SERVICE_ACCOUNT`** (JSON 문자열) → **`FIREBASE_SERVICE_ACCOUNT_PATH`**.

---

## 7. AI · LLM

### 7.1 모듈

- **`pipeline/summarizer.py`**: 기사 요약, 헤드라인 번역, 시황 리포트, US 기사 큐레이션, 기업 분석.
- 앱은 직접 LLM을 호출하지 않고 **Firestore에 적재된 결과**를 표시. 모델 선택값은 **`settings/ai_config`** 및 앱 DataStore의 **`AiSelectedModel`** (`claude` / `gemini`)과 개념 정렬.

### 7.2 파이프라인에서 쓰는 모델 상수 (코드 기준)

| 용도 | 기본/상수 |
|------|-----------|
| Gemini | `gemini-2.5-flash` (`GEMINI_MODEL`) |
| Claude | `claude-haiku-4-5-20251001` (`CLAUDE_MODEL`) |

`configure_ai(selected_model=...)` 로 런타임 분기.

### 7.3 기사 요약 동작 요약

- **`summarize_article`**: JSON 스키마 `headline`, `category`, `summary_points` — 카테고리는 고정 목록(경제, 테크&반도체, 증시, …) 및 별칭 정규화.
- **본문 유무**: `body`가 있으면 “기사 본문”을 주 근거로, 없으면 제목·짧은 summary 위주.
- **옵션** **`SAPIENS_ARTICLE_LLM_JUDGE`**: 켜면 Claude 초안 → Gemini 검증 루프(환경변수 `1`/`true` 등).

### 7.4 토큰 계측

- `summarizer.reset_token_counters()` / `get_token_usage()` — 파이프라인 `finally`에서 `pipeline_logs`에 기록.

### 7.5 앱 측 AI 관련 UI/설정

- DataStore 등으로 **`AiSelectedModel`** 노출 시 사용자 선택과 Firestore 설정을 맞추는 패턴(구체 화면은 `MyScreen` 등에서 확인).
- **알림**: FCM 토픽 `briefing_update`, `news_update`, `market_update` (`FcmTopicSync.kt`), 스케줄은 파이프라인에서 `push_schedule_util` + `firebase_client.write_push_schedule_entry`로 적재.

---

## 8. 앱이 직접 호출하는 외부 HTTP (예시)

`NewsRepositoryImpl` 등:

- **Daum 금융 API**: 국내 지수 (`api/quotes/...`).
- **Yahoo Finance** 스타일 엔드포인트: 해외 지수·원자재·환율 심볼 목록.
- **네이버 금융 / 공공데이터**: 종목 상세·테마·리서치 등 (`StockDetailRepositoryImpl`, `MStockApi`, `StockNaverNewsApi` 등).

---

## 9. 디자인 시스템 · UI 규격

### 9.1 원칙 (요약)

- **하드코딩 지양**: 색·간격·모서리는 `ui/theme/` 토큰과 `Spacing` / `AppShapes` / `Dimens` 사용.
- **Material3 `colorScheme`** + **Sapiens 런타임 색** (`Background`, `Card`, `TextPrimary` …) 병행.
- **폰트**: 현재 시스템 SansSerif — 주석상 추후 SUIT 예정 (`Typography.kt`).

### 9.2 테마 소스 파일

| 파일 | 내용 |
|------|------|
| `Theme.kt` | `SapiensTheme`, 라이트/다크 M3 스킴 |
| `Color.kt` | 브랜드색 `#F56E0F`, 팔레트, 칩 팔레트, `applyThemePalette` |
| `Typography.kt` / `Type.kt` | M3 스케일 / `SapiensTextStyles` |
| `Spacing.kt` / `Dimens.kt` / `Shape.kt` | 간격·카드 패딩·모서리 프리셋 |
| `common/ChipColors.kt` | 뉴스 카테고리 문자열 → `CategoryChipPalette` |

### 9.3 화면별 적용 요약

`color-type-guide.md` 참고: 브리핑 / 뉴스 / 마켓 / 마이 / 공통 바텀시트·로딩 등 텍스트 색·타이포 조합이 문서화되어 있음.

### 9.4 주요 Compose 컴포넌트·화면 파일 (참조용)

| 영역 | 파일 (예) |
|------|-----------|
| 브리핑 UI | `BriefingScreen.kt`, `BriefingComponents.kt`, `BriefingViewModel.kt` |
| 뉴스 UI | `NewsScreen.kt`, `NewsComponents.kt`, `NewsViewModel.kt`, `NewsRegionToggle` |
| 마켓 UI | `MarketScreen.kt`, `MarketViewModel.kt`, `StockDetailBottomSheet.kt`, `MarketSearchBottomSheet.kt` |
| 마이 | `MyScreen.kt`, `AuthViewModel.kt` |
| 공통 기사 | `ArticleBottomSheet.kt`, `ArticleBottomSheetKind.kt`, `ArticleOriginalUrl.kt`, `PdfViewerDialog.kt` |
| 시장 표시 | `MarketIndexStyleChangeText.kt` |

---

## 10. 인증·동기화·알림

- **Firebase Auth** + **Google Sign-In** (`play-services-auth`, `strings.xml`의 Web client ID는 OAuth용).
- **FCM**: 채널 `sapiens_news`, 토픽은 `FcmTopicSync`와 파이프라인 스케줄의 `topic` 필드 일치.
- **POST_NOTIFICATIONS**: Android 13+ 권한 선언 및 런타임 처리 (`FcmTopicSync.hasNotificationPermission`).

---

## 11. 보안·운영 가드레일

- 서비스 계정·`google-services.json` **Git 커밋 금지** (Push protection 대비).
- `META-INF` 중복 패키징 `pickFirsts` 설정 유지 (`app/build.gradle.kts`).
- 상세: **`docs/app-guardrails-ko.md`**.

---

## 12. 관련 문서 (저장소 내)

| 파일 | 용도 |
|------|------|
| `design system.md` | 토큘·타이포·간격·Shape 상세 |
| `color-type-guide.md` | 화면별 컬러·타이포 적용 |
| `docs/app-guardrails-ko.md` | CI, Lint, Firebase 로컬 설정 |

---

## 13. 변경 시 유지보수 노트

- Firestore 문서 ID·필드명을 바꾸면 **`NewsRepositoryImpl`**, **`FirestoreMappers`**, **`firebase_client.py`**, **`summarizer.merge_to_firestore_article`** 를 함께 점검.
- 뉴스 카테고리 라벨을 바꾸면 **`summarizer.py`** 의 허용 카테고리와 **`ChipColors.kt`** 를 동기화.
- 디자인 토큘 변경 시 **`design system.md`** / **`color-type-guide.md`** 동반 갱신 권장.

---

*문서 생성 기준: 저장소 소스 트리 스캔. 버전 번호·의존성 버전은 `app/build.gradle.kts` 및 `gradle/libs.versions.toml`이 다를 수 있으므로 배포 전 실제 Gradle 해석 결과를 확인하세요.*

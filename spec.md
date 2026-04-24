# Sapiens — 프로젝트 스펙 (현행 반영)

> 한국 투자자용 뉴스·마켓 정보 Android 앱 + Python 크롤/요약 파이프라인 + Firebase(Firestore)  
> **최종 업데이트: 2026-04-23**

이 문서는 **현재 저장소에 구현·반영된 동작**을 기준으로 한다. (과거 DailyNews 브라우저 프로토타입 스펙은 본 문서 범위가 아님.)

---

## 1. 한 줄 요약

- **앱**: Kotlin + Jetpack Compose, Material 3. 하단 탭 **뉴스 / 마켓 / 마이**.
- **데이터**: Firestore DB ID `sapiens`. 뉴스 피드·마켓 테마/업종·지표 등은 파이프라인이 적재하고 앱이 구독.
- **파이프라인**: RSS·네이버 등 크롤 → 탭별 지정 RSS만 `news` 문서에 적재 → Claude(Haiku)로 **기사 요약** → Firestore 저장. `PIPELINE_SECTION`으로 일부만 실행 가능.

---

## 2. 기술 스택 (요약)

| 영역 | 내용 |
|------|------|
| 앱 | Android (minSdk 26), Kotlin, Compose, Material3, Firebase Auth/Firestore/FCM, Coil, Retrofit/OkHttp, DataStore |
| 백엔드(서버리스) | Firebase Firestore, FCM 토픽·예약 푸시(`push_schedule_util` 등) |
| 파이프라인 | Python 3, `feedparser`, BeautifulSoup, Playwright(토스 등), Anthropic Claude(`anthropic`), `python-dotenv` |
| LLM | 기사 요약·시황 등: **Claude Haiku** (`pipeline/summarizer.py`의 `CLAUDE_MODEL`, 기본 `claude-haiku-4-5-20251001`). `ANTHROPIC_API_KEY` 필수. (뉴스 탭 소스는 RSS 지정으로 고정, LLM 탭 분류 없음.) |

---

## 3. 저장소 구조 (상위)

```
Sapiens/
├── app/                    # Android 앱 모듈
├── pipeline/               # 크롤·요약·Firestore 적재
├── gradle/
├── build.gradle.kts
└── spec.md                 # 본 문서
```

---

## 4. Android 앱 — 현재 화면·동작

### 4.1 내비게이션

- `MainScreen`: 하단 탭 **뉴스(0) / 마켓(1) / 마이(2)**.
- 딥링크/알림 등에서 `navigateToSectionKey`로 탭 전환 가능 (`market`, `news` 등).

### 4.2 뉴스 탭

- Firestore `news` 컬렉션 문서 **`domestic_market`**, **`global_market`**, **`ai_issue`** 구독.
- 탭당 표시 기사 상한: **`NEWS_FEED_MAX_ARTICLES = 9`** (`NewsRepositoryImpl.kt`). 스냅샷의 `articles` 배열 앞에서 9건만 사용.
- 기사 행 탭 시 **`ArticleBottomSheet`**: 제목, 시간, 요약 포인트(최대 4블릿·`summaryPoints` 없으면 `summary`에서 파생), **기사 원문 보기**(URL 있을 때).
- **한자 표기 → 한글 표기**: Firestore에서 `Article`로 매핑할 때 및 북마크 JSON 복원 시 `normalizeNewsTextForDisplay()` 적용 (`koalanlp-core` + Java 브리지 `HanjaExt`, Kotlin 2.x와 구 라이브러리 메타데이터 이슈 회피).

### 4.3 마켓 탭

- 테마 / 업종 서브탭, Firestore `market/themes/by_no/{no}`, `market/industries/by_no/{no}` 등에서 테마 카드·종목 행 표시.
- **종목명 클릭 시 종목 상세 바텀시트 없음** (제거됨). 종목명은 표시만, 별도 상세 플로우 없음.
- 테마 설명 등: `ThemeDescriptionRepository` → `StockRetrofitProvider.stockNaverTheme`(네이버 stock API)만 사용.

### 4.4 마이 탭

- Google 로그인(`AuthViewModel`), 북마크·피드백·클라우드 백업 관련 UI 등 (세부는 코드·`MyScreen` 참고).

### 4.5 제거·미사용 (앱 쪽, 과거 대비)

다음은 **현재 코드베이스에 없음** (요청에 따라 삭제되었거나 미연결 UI).

- 종목 상세: `StockDetailBottomSheet`, `StockDetailViewModel`, `StockDetailRepository`/`StockDetailModels`, 공공데이터/KRX/m.stock/네이버 리서치·뉴스 API 등 종목 전용 Retrofit 일괄.
- `BuildConfig.PUBLIC_DATA_API_KEY` 및 `local.properties`의 `PUBLIC_DATA_API_KEY` 주입, `buildFeatures.buildConfig` (전용 필드 제거 후 불필요 시 비활성).
- `CompanyBottomSheet`, `MarketSearchBottomSheet` (참조 없이 삭제됨).

---

## 5. Firestore (요약)

- **Database ID**: 앱에서 `FirebaseFirestore.getInstance(..., "sapiens")` 형태로 **`sapiens`** 사용.
- **뉴스**: `news/domestic_market|global_market|ai_issue` — 문서당 `articles` 배열, 앱 `Article` 필드와 매퍼 `FirestoreMappers.parseArticles`.
- **마켓**: `market/indicators`, `market/themes/by_no/*`, `market/industries/by_no/*` 등 (`firebase_client.py` 상수와 정리).
- **레거시 문서 정리**: `clear_news_feeds` 등에서 `overseas_stocks`, `overseas_tech`, `realtime`, `popular`, `main`, 브리핑용 `briefing` 잔여 ID 등 삭제 대상이 코드에 명시됨.
- **`companies/{ticker}` 저장**: 파이프라인에서 **제거됨** (`save_company_data` 삭제, 종목 크롤·`analyze_company` 제거).

---

## 6. Python 파이프라인 — 현재

### 6.1 진입점

- `pipeline/main.py` → `run()`: `.env` 로드 후 `PIPELINE_SECTION`에 따라
  - **`all`**: 뉴스 블록 + 마켓 블록(각각 휴장 규칙), `full`과 유사하나 구성은 코드 기준.
  - **`news`**: 뉴스만.
  - **`market`**: 지표 없이 테마/업종만 등 (`_run_market_only`).
  - **`full`**: 뉴스 피드 초기화 시도 후 전체 크롤·요약·지표·테마·푸시 예약 등.
- `domestic_news` → `news`로 하위 호환 매핑.

### 6.2 뉴스 크롤·탭 소스

- **`crawl_domestic`**: 탭마다 **지정 RSS만** 읽어 `domestic_market` / `global_market` / `ai_issue` 문서에 넣는다. 풀을 합쳐 LLM으로 탭을 나누지 않는다.
  - **국내증시**: `RSS_FEEDS_NEWS_KR_MARKET`(매경 증권·한경 finance).
  - **미국증시**: `RSS_FEEDS_NEWS_CNBC_MARKETS`(CNBC Markets, URL `cnbc.com`만).
  - **AI 이슈**: `RSS_FEEDS_NEWS_AI`(CNBC Tech).
- `RSS_FEEDS_NEWS_OVERSEAS` 등은 이 경로에서 사용하지 않는다(필요 시 별도 호출·기능에 연결).
- 레거시 LLM 탭 분류 프롬프트는 `news_tab_classification.py`에 남아 있으나 `crawl_domestic`에서는 호출하지 않는다.
- 탭별 기사 상한(크롤 후 슬라이스): **`RSS_DOMESTIC_NEWS_MAX_ITEMS = 9`** (`crawler.py`) — Firestore `articles` 길이와 앱 `take(9)`와 맞춤.

### 6.3 제거된 파이프라인 기능

- **종목 단위 크롤·저장**: `crawl_all_company_bundles`, 국내/해외 Yahoo·네이버 재무·증권가 블록, `summarizer.analyze_company`, `firebase_client.save_company_data`, **`company_crawler.py` 파일** 삭제.

### 6.4 기사 요약 (규칙 요약)

- **모델**: Claude (`summarizer.py`, `CLAUDE_MODEL`).
- **출력 JSON 키**: `headline`, `category`, `summary_points`.
- **규칙**: 한국어 제목, 고정 카테고리 목록·가이드, 수치·고유명 유지, 추상 대체 금지, 쉬운 말 풀이 등(프롬프트 문자열 참고).
- **후처리**: 영문 제목만 보이면 별도 호출로 한국어 헤드라인 번역, `summary_points` 최대 10개까지, 카테고리 별칭·미허용 시 `경제` 등.
- **환경변수**: `SAPIENS_ARTICLE_LLM_JUDGE=1` 시 단일 Claude judge 경로(재시도 2회) 우선 등.

### 6.5 기타

- `summarize_batch`: 기사 간 **0.5초** 간격.
- `merge_to_firestore_article`: 앱 스키마에 맞게 `headline`/`summaryPoints`/`summary`(원문 리드 500자 자름) 등 병합.

---

## 7. 앱 빌드·구성 메모

- **Google Services**: `app/google-services.json` 필요.
- **한자 변환**: `implementation("kr.bydelta:koalanlp-core:2.1.4")` + `com.sapiens.app.data.text.HanjaExt` (Java).
- **검증 태스크**: `./gradlew :app:verifyApp` (lint 포함, `app/build.gradle.kts` 참고).

---

## 8. 빠른 참조 (파일)

| 주제 | 위치 |
|------|------|
| 뉴스 피드 구독·9건 자르기 | `app/.../NewsRepositoryImpl.kt` |
| Firestore → Article 매핑·한자 치환 | `app/.../FirestoreMappers.kt` |
| 기사 바텀시트 UI | `app/.../ArticleBottomSheet.kt` |
| 마켓 화면(종목 클릭 없음) | `app/.../market/MarketScreen.kt` |
| 메인 탭 셸 | `app/.../main/MainScreen.kt` |
| 네이버 테마 API만 Retrofit | `app/.../stock/StockRetrofitProvider.kt` |
| 파이프라인 실행·섹션 | `pipeline/main.py` |
| 크롤·탭 상한 9 | `pipeline/crawler.py` (`RSS_DOMESTIC_NEWS_MAX_ITEMS`) |
| 요약·Claude | `pipeline/summarizer.py` |
| Firestore 쓰기 | `pipeline/firebase_client.py` |

---

## 9. 문서 유지

- 제품·아키텍처 변경 시 **본 `spec.md`를 함께 갱신**하는 것을 권장한다.
- 과거 **DailyNews HTML 프로토타입** 스펙은 이 저장소의 진실 공급원(truth)이 아니다.

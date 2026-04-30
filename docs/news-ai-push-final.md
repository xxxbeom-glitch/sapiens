## Sapiens 뉴스 → AI 요약 → 푸시 “최종안” 정리

이 문서는 **현재 앱 코드 기준**으로, 뉴스(기사) 데이터가 **어디서 어떻게 로드되고**, AI가 **어떤 방식으로 요약을 만들며**, 그 결과가 **어떻게 푸시로 전달되는지**를 한 번에 이해할 수 있도록 정리한 “최종안”입니다.

---

## 핵심 결론 (현재 앱 기준)

- **기사 데이터 로드(앱)**: 앱은 **Firestore 문서(`news/{documentId}`)** 를 **실시간 구독**해서 `articles` 배열을 UI에 올립니다.  
  - 근거: `NewsRepositoryImpl.getNewsFeed()` / `getNewsFeedDocument()`  
  - 파일: `app/src/main/java/com/sapiens/app/data/repository/NewsRepositoryImpl.kt`

- **AI 요약 생성(앱 안에서 수행하지 않음)**: 앱 내부에는 “LLM 호출(Claude/OpenAI/Gemini 등)”로 요약을 생성하는 코드가 없습니다.  
  - 앱은 **이미 요약이 포함된 기사(`summary`, `summaryPoints`)** 를 Firestore에서 받아 화면에 표시합니다.
  - 앱에서 가능한 AI 관련 동작은 **선택 모델 값을 Firestore 설정에 저장**하는 것뿐입니다.  
  - 근거: `AiConfigFirestoreRepository`, `AiSelectedModel`  
  - 파일: `app/src/main/java/com/sapiens/app/data/repository/AiConfigFirestoreRepository.kt`, `app/src/main/java/com/sapiens/app/data/model/AiSelectedModel.kt`

- **푸시(FCM)**:
  - 앱은 `sapiens_feed` 토픽을 구독/해제하고, 푸시를 받으면 포그라운드에서는 로컬 알림을 표시합니다.
  - “언제/무슨 기사로 푸시를 쏘는지”는 앱이 아니라 **Cloud Functions/외부 파이프라인**이 결정합니다(앱에는 발송 로직이 없음).
  - 근거: `FcmTopicSync`, `SapiensFirebaseMessagingService`
  - 파일: `app/src/main/java/com/sapiens/app/messaging/FcmTopicSync.kt`, `app/src/main/java/com/sapiens/app/SapiensFirebaseMessagingService.kt`

---

## 1) 기사 데이터는 어떻게 불러오나 (앱)

### 1.1 데이터 소스
- Firestore 데이터베이스: `sapiens`
- 컬렉션/문서:
  - `news/{documentId}` (문서에 `articles` 배열)
  - `market/...` (테마/지표 등; 뉴스 외 참고)

### 1.2 로딩 방식: 실시간 구독
앱은 REST로 “뉴스를 크롤링”하거나 직접 외부 뉴스 API를 호출하지 않고, **Firestore 문서를 snapshot listener로 구독**합니다.

- 구현:
  - `getNewsFeed(type: NewsFeedType): Flow<List<Article>>`
  - `getNewsFeedDocument(documentId: String): Flow<List<Article>>`
  - `FirebaseFirestore.collection("news").document(docId).addSnapshotListener(...)`
  - `articles` 필드를 `Article` 리스트로 파싱 후 최대 15개로 제한

- 근거 코드:
  - `NewsRepositoryImpl.kt` (예: `NEWS_FEED_MAX_ARTICLES = 15`, `parseArticles("articles")`)
  - `FirestoreMappers.kt` (Map → `Article` 파싱)

### 1.3 UI 연결
`NewsViewModel`이 선택된 문서 id를 기준으로 `repository.getNewsFeedDocument(docId)`를 `StateFlow`로 구독하고 화면에 제공합니다.

- 파일:
  - `app/src/main/java/com/sapiens/app/ui/news/NewsViewModel.kt`

---

## 2) 기사 모델 스키마 (앱이 기대하는 형태)

`Article`은 “원문 + 요약”이 이미 포함된 형태입니다.

- 파일: `app/src/main/java/com/sapiens/app/data/model/Article.kt`
- 필드(핵심):
  - `headline` / `headline_ko`(Firestore에서 우선 사용)  
  - `summary` (요약 텍스트)
  - `summaryPoints` (요약 포인트 리스트; 없으면 UI에서 `summary`를 쪼개 임시 생성)
  - `time`, `source`, `url` 등

Firestore 파싱은 아래 로직으로 수행됩니다.

- 파일: `app/src/main/java/com/sapiens/app/data/repository/FirestoreMappers.kt`
  - `headline_ko`가 있으면 우선 사용
  - `summaryPoints`는 `summaryPoints` 배열을 읽고 `normalizeNewsTextForDisplay()` 적용

---

## 3) AI는 어떤 모델로, 어떤 코드로, 어떻게 요약하나?

### 3.1 앱 내 “요약 생성”은 없음 (중요)
현재 코드베이스(앱 모듈)에는:
- Claude/OpenAI/Gemini 같은 LLM API 호출 코드
- 프롬프트 템플릿
- 요약 생성 요청/응답 처리

가 **존재하지 않습니다**.

즉, **요약은 앱 밖(서버/파이프라인)** 에서 생성되어 Firestore의 `news/{docId}.articles[].summary` 및 `summaryPoints`에 저장되고, 앱은 이를 **표시만** 합니다.

### 3.2 앱이 하는 AI 관련 설정: “선택 모델” 저장
앱에는 “파이프라인과 동일 스키마”로 모델 선택을 저장하는 코드가 있습니다.

- 파일: `app/src/main/java/com/sapiens/app/data/repository/AiConfigFirestoreRepository.kt`
  - 문서: `settings/ai_config`
  - 필드: `selected_model`, `updatedAt`

- 모델 값 정규화:
  - 파일: `app/src/main/java/com/sapiens/app/data/model/AiSelectedModel.kt`
  - `gemini` 등 레거시는 모두 `claude`로 normalize (주석상 파이프라인은 Claude 기준)

### 3.3 UI에서 summaryPoints 생성(대체 로직)
`summaryPoints`가 비어있을 경우, UI에서 `summary`를 문장 단위로 잘라 임시 포인트를 만듭니다.

- 파일: `app/src/main/java/com/sapiens/app/ui/common/ArticleBottomSheet.kt`
  - `article.summaryPoints ?: buildSummaryPoints(summary, headline)`

> 이 로직은 “AI 요약 생성”이 아니라, 이미 존재하는 텍스트를 **표시용으로 분해**하는 보조 로직입니다.

---

## 4) 앱은 요약 결과를 어떻게 “푸시로” 전달(수신)하나

### 4.1 토픽 구독
앱은 Cloud Functions/파이프라인과 동일한 토픽을 구독합니다.

- 파일: `app/src/main/java/com/sapiens/app/messaging/FcmTopicSync.kt`
  - 토픽: `sapiens_feed`
  - 레거시 토픽: `news_update`, `market_update` (구독 해제 처리 포함)
  - DataStore의 `pushNotificationsEnabled` 및 Android 13+ 알림 권한에 따라 subscribe/unsubscribe

또한 `MainActivity`에서 환경설정 변화에 맞춰 `FcmTopicSync.syncFromPreference(...)`를 호출합니다.

- 파일: `app/src/main/java/com/sapiens/app/MainActivity.kt`

### 4.2 메시지 수신 처리 (포그라운드)
FCM 수신 시:
- `notification` 페이로드가 있으면 그것을 사용
- 없으면 `data["title"]`, `data["body"]` 사용
- `data["section"]`이 있으면 앱 진입 시 해당 섹션으로 이동시키기 위한 extra로 전달

- 파일: `app/src/main/java/com/sapiens/app/SapiensFirebaseMessagingService.kt`

### 4.3 “발송”은 어디서?
앱 코드에는 `FirebaseMessaging.send(...)` 같은 발송 코드가 없고, 토픽 이름 주석에도 명시되어 있습니다.

- `FcmTopicSync.kt` 주석: “Cloud Functions / 파이프라인에서 발송”

따라서 “어떤 기사로 언제 푸시를 보낼지”는 **서버/파이프라인**의 책임입니다.

---

## 5) 최종 아키텍처(현재 앱 + 외부 파이프라인) 요약

### 5.1 데이터 흐름(권장 최종안)
1. (외부) 뉴스 수집/필터링 파이프라인이 원문을 수집
2. (외부) 선택된 모델(`settings/ai_config.selected_model`, 기본 `claude`)로 기사 요약 생성
3. (외부) 결과를 Firestore에 저장
   - `news/{docId}.articles[]`에 `headline`, `summary`, `summaryPoints`, `url`, `time` 등을 포함
4. (앱) Firestore 문서를 실시간 구독 → 화면 반영
5. (외부) “새 피드 발행” 이벤트에서 `sapiens_feed` 토픽으로 FCM 발송
6. (앱) 수신 시 포그라운드면 로컬 알림 표시 + `section`으로 라우팅

---

## 6) 운영 체크리스트(앱 관점)

- **Firestore 문서 스키마**: `articles` 배열 요소에 `headline`(또는 `headline_ko`), `summary`, `time`은 필수에 가깝습니다. (`summary`/`time`이 없으면 파싱에서 `Article`이 null 처리됨)
- **요약 포인트**: `summaryPoints`가 비어도 UI가 깨지지 않게 `buildSummaryPoints()`가 대체합니다.
- **푸시**: Android 13+에서는 `POST_NOTIFICATIONS` 권한 및 DataStore 설정에 따라 토픽 구독이 달라집니다.
- **AI 모델 선택**: 앱이 저장하는 값은 현재 normalize로 `claude`로 귀결됩니다(레거시 포함).

---

## 부록: 주요 파일 목록

- **뉴스 로드**
  - `app/src/main/java/com/sapiens/app/data/repository/NewsRepositoryImpl.kt`
  - `app/src/main/java/com/sapiens/app/data/repository/FirestoreMappers.kt`
  - `app/src/main/java/com/sapiens/app/ui/news/NewsViewModel.kt`

- **AI 설정(모델 선택)**
  - `app/src/main/java/com/sapiens/app/data/repository/AiConfigFirestoreRepository.kt`
  - `app/src/main/java/com/sapiens/app/data/model/AiSelectedModel.kt`

- **푸시 수신/토픽**
  - `app/src/main/java/com/sapiens/app/messaging/FcmTopicSync.kt`
  - `app/src/main/java/com/sapiens/app/SapiensFirebaseMessagingService.kt`
  - `app/src/main/java/com/sapiens/app/MainActivity.kt`


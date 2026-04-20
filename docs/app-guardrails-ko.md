# Android `app` 모듈 — 오류가 반복되는 이유와 방지

## 왜 같은 문제가 다시 보이나

1. **로컬은 되는데 CI/다른 PC에서 실패**  
   `google-services.json`은 `.gitignore`라 저장소에 없음 → 클론만 한 환경에서는 Firebase 관련 Gradle 태스크가 실패할 수 있음.  
   **대응**: CI는 `tools/google-services.ci.json`을 잠시 복사해 **빌드·Lint만** 통과시킵니다(실서비스 키 아님).

2. **Lint는 IDE에서 안 보이다가 `./gradlew lintDebug`에서만 터짐**  
   예: Android 13+ `POST_NOTIFICATIONS` 없이 `notify()` 호출 → `MissingPermission`.  
   **대응**: 푸시 전에 항상 `:app:verifyApp` 실행.

3. **`git status`에 `app/build/...`가 수천 줄**  
   빌드 산출물이 추적되면 리뷰·머지가 지저분해지고, 실수로 커밋될 위험이 있음.  
   **대응**: 루트 `.gitignore`에 `app/build/` 명시.

4. **GitHub Push Protection (시크릿 차단)**  
   `*-firebase-adminsdk-*.json`, 서비스 계정 JSON 등이 커밋에 들어가면 푸시가 거절됨.  
   **대응**: 이미 `.gitignore` 패턴 유지 + **절대** 루트에 키 파일 두지 않기.

5. **의존성 충돌(META-INF 중복)**  
   구 Support 라이브러리와 AndroidX가 같이 끌려올 때 `mergeDebugJavaResource` 실패.  
   **대응**: `app/build.gradle.kts`의 `packaging { resources { pickFirsts += … } }` 유지.

---

## 앞으로 오류를 줄이는 습관

| 언제 | 무엇을 할지 |
|------|-------------|
| **푸시/PR 전** | 저장소 루트에서 `.\gradlew.bat :app:verifyApp` **또는** `.\scripts\verify-app.ps1` |
| **실제 Firebase로 로컬 실행** | `app/google-services.json`은 로컬에만 두고 커밋하지 않기 |
| **파이프라인·서버 키** | Git에 넣지 말고 GitHub Secrets / 로컬 `.env` / base64 환경변수만 사용 |
| **PR 올린 뒤** | GitHub Actions **Android app verify** 워크플로가 초록인지 확인 |

---

## 명령 요약

```powershell
# 권장 (google-services 없을 때 스크립트가 CI용 JSON만 임시 사용)
.\scripts\verify-app.ps1

# 또는 (이미 app/google-services.json 이 있는 경우와 동일)
.\gradlew.bat :app:verifyApp
```

`verifyApp` = **`lintDebug`** (내부적으로 컴파일·리소스 처리 포함). Lint **에러**가 있으면 실패합니다.

---

## CI

- 파일: `.github/workflows/android-app-pr.yml`
- `app/**` 등 변경이 있는 PR·`main` 푸시 시 `tools/google-services.ci.json` → `app/google-services.json` 복사 후 `:app:verifyApp` 실행.

---

## 문제가 나면

1. 로컬에서 `.\scripts\verify-app.ps1` 로그 확인  
2. `app/build/reports/lint-results-debug.html` 열기  
3. Gradle: `./gradlew :app:assembleDebug --stacktrace`

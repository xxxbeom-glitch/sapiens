package com.sapiens.app.data.model

/** Firestore `settings/ai_config` · DataStore. 파이프라인은 `gemini` 전용. */
object AiSelectedModel {
    const val GEMINI = "gemini"

    /** 이전 claude 값도 모두 gemini로 통일(파이프라인은 Gemini 전용). */
    fun normalize(raw: String?): String = GEMINI
}

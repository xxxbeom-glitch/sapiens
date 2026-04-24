package com.sapiens.app.data.model

/** Firestore `settings/ai_config` · DataStore. 파이프라인은 Gemini(`gemini`) 기준. */
object AiSelectedModel {
    const val GEMINI = "gemini"
    /** 레거시 값. [normalize]에서 gemini로 통일. */
    const val CLAUDE = "claude"

    /** 레거시 claude·미지정은 모두 gemini로 통일(파이프라인은 Gemini). */
    fun normalize(raw: String?): String {
        val s = raw?.trim()?.lowercase().orEmpty()
        return when (s) {
            GEMINI, CLAUDE, "" -> GEMINI
            else -> GEMINI
        }
    }
}

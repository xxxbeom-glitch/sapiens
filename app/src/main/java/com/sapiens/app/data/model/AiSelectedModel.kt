package com.sapiens.app.data.model

/** Firestore `settings/ai_config` · DataStore. 파이프라인은 Claude Haiku(`claude`) 기준. */
object AiSelectedModel {
    const val CLAUDE = "claude"
    /** 레거시 값. [normalize]에서 claude로 통일. */
    const val GEMINI = "gemini"

    /** 레거시 gemini·미지정은 모두 claude로 통일(파이프라인은 Claude Haiku). */
    fun normalize(raw: String?): String {
        val s = raw?.trim()?.lowercase().orEmpty()
        return when (s) {
            CLAUDE, GEMINI, "" -> CLAUDE
            else -> CLAUDE
        }
    }
}

package com.sapiens.app.data.model

/** Firestore `settings/ai_config` · DataStore와 동일한 값. */
object AiSelectedModel {
    const val CLAUDE = "claude"
    const val GEMINI = "gemini"

    fun normalize(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            CLAUDE -> CLAUDE
            else -> GEMINI
        }
    }
}

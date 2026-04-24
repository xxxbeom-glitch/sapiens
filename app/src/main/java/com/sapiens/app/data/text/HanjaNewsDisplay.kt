package com.sapiens.app.data.text

/** 뉴스 UI용: 한자 표기를 국문(한글) 표기로 바꿉니다. (국사편찬위원회 한자음가 사전, koalanlp-core) */
internal fun CharSequence.normalizeNewsTextForDisplay(): String =
    HanjaExt.toHangulForDisplay(this)

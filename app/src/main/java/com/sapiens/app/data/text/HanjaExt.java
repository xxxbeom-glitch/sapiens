package com.sapiens.app.data.text;

import androidx.annotation.NonNull;
import kr.bydelta.koala.ExtUtil;

/** Kotlin 2.x 가 구버전 Kotlin 메타데이터의 {@code ExtUtil} 직접 참조에 실패해, Java에서 koalanlp 호출만 담당합니다. */
public final class HanjaExt {
    private HanjaExt() {}

    @NonNull
    public static String toHangulForDisplay(@NonNull CharSequence text) {
        if (text.length() == 0) return "";
        try {
            return ExtUtil.hanjaToHangul(text).toString();
        } catch (Throwable ignored) {
            return text.toString();
        }
    }
}

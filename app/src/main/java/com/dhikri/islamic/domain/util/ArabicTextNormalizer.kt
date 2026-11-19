package com.dhikri.islamic.domain.util

object ArabicTextNormalizer {
    private val diacriticsRegex = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640\u08D3-\u08FF\uFE70-\uFEFF]")
    private val alefVariantsRegex = Regex("[إأآٱ\u0671]")
    private val multiSpaceRegex = Regex("\\s+")

    fun normalizeForSearch(input: String): String {
        return input
            .replace(diacriticsRegex, "")
            .replace(alefVariantsRegex, "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace(multiSpaceRegex, " ")
            .trim()
    }
}

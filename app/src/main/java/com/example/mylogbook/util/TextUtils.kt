package com.example.mylogbook.util

object TextUtils {
    private val multiSpaceRegex = Regex("\\s+")

    fun normalizeSpaces(value: String): String {
        return value.trim().replace(multiSpaceRegex, " ")
    }
}

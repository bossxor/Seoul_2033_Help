package com.bossxor.seoul2033help.domain

object TextNormalizer {
    private val keep = Regex("[\\p{L}\\p{N}]+")

    fun normalize(s: String): String =
        keep.findAll(s.lowercase())
            .joinToString("") { it.value }
}

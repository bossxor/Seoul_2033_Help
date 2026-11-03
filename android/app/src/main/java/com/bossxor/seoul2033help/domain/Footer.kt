package com.bossxor.seoul2033help.domain

/**
 * 게임 화면 선택지 아래 푯말 줄: `[이야기] 무지개… - 169 -`
 * 분류(tag), 잘린 제목 앞부분(titlePrefix), 페이지 번호(page)를 읽어 제목 매칭에 쓴다.
 */
data class Footer(val tag: String?, val titlePrefix: String?, val page: Int?) {
    companion object {
        private val pageRe = Regex("[-–—]\\s*(\\d{1,5})\\s*[-–—]")
        private val ellipsis = Regex("[…·.]{1,3}\\s*$")

        fun parse(text: String): Footer? {
            var rest = text.trim()
            if (rest.isEmpty()) return null
            val page = pageRe.find(rest)?.groupValues?.get(1)?.toIntOrNull()
            rest = pageRe.replace(rest, " ").trim()
            val tokens = rest.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return if (page != null) Footer(null, null, page) else null
            // 분류 칩이 있으면 첫 토큰이 분류, 나머지가 잘린 제목
            val hasTag = tokens.size >= 2
            val tag = if (hasTag) tokens.first() else null
            val title = tokens.drop(if (hasTag) 1 else 0).joinToString(" ").replace(ellipsis, "").trim()
            return Footer(tag, title.ifEmpty { null }, page)
        }
    }
}

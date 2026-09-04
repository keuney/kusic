package com.keuney.music.ui.format

/**
 * 캐시 크기를 화면에 쓰는 짧은 글자로 만든다.
 *
 * 1024를 단위로 센다. 캐시 상한을 그렇게 정했으므로(128MB = 128 * 1024 * 1024) 같은 기준으로
 * 세지 않으면 고른 값과 보이는 값이 어긋난다.
 *
 * MB부터는 소수점을 버린다. 캐시 크기는 얼마나 찼는지 보려는 값이고 소수점 첫째 자리가 판단을
 * 바꾸지 않는다. KB 미만은 그냥 0KB로 보인다.
 */
internal fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0)
    val kb = safe / 1024
    if (kb < 1024) return "${kb}KB"
    val mb = kb / 1024
    if (mb < 1024) return "${mb}MB"
    return "${mb / 1024}.${(mb % 1024) * 10 / 1024}GB"
}

package cloud.mindbox.mobile_sdk

import android.util.Log
import cloud.mindbox.mobile_sdk.utils.LoggingExceptionHandler
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun Map<String, String>.toUrlQueryString() = LoggingExceptionHandler.runCatching(
    defaultValue = ""
) {
    this.map { (k, v) -> "$k=$v" }
        .joinToString(prefix = "?", separator = "&")
}

internal fun String.convertToLongDateMilliSeconds(): Long = runCatching {
    return LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).atZone(
        ZoneId.systemDefault()
    ).toEpochSecond() * 1000
}.getOrElse {
    Log.e("Mindbox", "Error converting date", it)
    0L
}

internal fun Long.convertToStringDate(): String = runCatching {
    Log.e("date", Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
}.getOrThrow()/*.getOrElse {
    Log.e("Mindbox", "Error converting date", it)
    ""
}*/

internal fun <T> List<T>.subListInclusive(fromIndex: Int, toIndex: Int): List<T> {
    return this.subList(fromIndex, toIndex) + this[toIndex]
}

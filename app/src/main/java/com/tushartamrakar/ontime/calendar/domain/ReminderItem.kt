package com.tushartamrakar.ontime.calendar.domain

data class ReminderItem(
    val type: String = "NOTIFICATION", // "NOTIFICATION" or "ALARM"
    val minutesBefore: Int = 10,
    val sound: String = "alarm_digital_alarm",
)

fun List<ReminderItem>.toJson(): String {
    if (isEmpty()) return "[]"
    val items = joinToString(",") { r ->
        """{"type":"${r.type}","minutesBefore":${r.minutesBefore},"sound":"${r.sound}"}"""
    }
    return "[$items]"
}

fun String.toReminderItems(): List<ReminderItem> {
    if (this == "[]" || this.isBlank()) return emptyList()
    return try {
        val items = mutableListOf<ReminderItem>()
        val clean = this.trim().removePrefix("[").removeSuffix("]")
        if (clean.isBlank()) return emptyList()
        // Simple JSON parsing without Gson dependency
        val pattern = Regex("""\{[^}]+\}""")
        pattern.findAll(clean).forEach { match ->
            val obj = match.value
            val type = Regex(""""type":"([^"]+)"""").find(obj)?.groupValues?.get(1) ?: "NOTIFICATION"
            val mins = Regex(""""minutesBefore":(\d+)""").find(obj)?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val sound = Regex(""""sound":"([^"]+)"""").find(obj)?.groupValues?.get(1) ?: "alarm_digital_alarm"
            items.add(ReminderItem(type, mins, sound))
        }
        items
    } catch (e: Exception) {
        emptyList()
    }
}

fun Int.toReminderLabel(): String = when {
    this < 60 -> "${this}m before"
    this == 60 -> "1h before"
    this < 1440 -> "${this / 60}h before"
    this == 1440 -> "1 day before"
    this == 2880 -> "2 days before"
    this == 10080 -> "1 week before"
    else -> "${this}m before"
}

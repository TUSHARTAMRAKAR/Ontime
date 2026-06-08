package com.tushartamrakar.ontime.alarm.domain

import org.json.JSONArray
import org.json.JSONObject

// ─── Task Types ───────────────────────────────────────────────────────────────
enum class TaskType {
    MATH, TYPING, SHAKE, BARCODE
}

enum class MathDifficulty(val label: String, val description: String) {
    EASY("Easy", "Simple +/- with small numbers"),
    MEDIUM("Medium", "×/÷ with medium numbers"),
    HARD("Hard", "Mixed operations with large numbers"),
}

enum class TypingCategory(val label: String, val emoji: String) {
    MOTIVATIONAL("Motivational", "💪"),
    AFFIRMATIONS("Affirmations", "🌟"),
    TONGUE_TWISTERS("Tongue Twisters", "👅"),
    CUSTOM("Custom", "✏️"),
}

// ─── Simple task data class ───────────────────────────────────────────────────
data class WakeUpTask(
    val type: TaskType,
    val mathDifficulty: String = MathDifficulty.EASY.name,
    val mathQuestionCount: Int = 3,
    val typingCategory: String = TypingCategory.MOTIVATIONAL.name,
    val typingCustomPhrases: List<String> = emptyList(),
    val shakeCount: Int = 30,
    val barcodes: List<BarcodeItem> = emptyList(),
)

data class BarcodeItem(
    val id: String,
    val name: String,
    val barcode: String,
)

// ─── Task UI Info ─────────────────────────────────────────────────────────────
data class TaskInfo(
    val type: TaskType,
    val emoji: String,
    val name: String,
    val description: String,
    val category: String,
)

val availableTasks = listOf(
    TaskInfo(
        type = TaskType.MATH,
        emoji = "🧮",
        name = "Math Challenge",
        description = "Solve arithmetic questions to dismiss",
        category = "Wake Brain",
    ),
    TaskInfo(
        type = TaskType.TYPING,
        emoji = "⌨️",
        name = "Typing Task",
        description = "Type a phrase or quote exactly",
        category = "Wake Brain",
    ),
    TaskInfo(
        type = TaskType.SHAKE,
        emoji = "📳",
        name = "Shake Phone",
        description = "Shake your phone to dismiss",
        category = "Wake Body",
    ),
    TaskInfo(
        type = TaskType.BARCODE,
        emoji = "📷",
        name = "Barcode Scan",
        description = "Scan a registered item's barcode",
        category = "Wake Up",
    ),
)

// ─── JSON serialization using Android's built-in JSONObject ──────────────────
fun WakeUpTask.toJson(): JSONObject {
    return JSONObject().apply {
        put("type", type.name)
        put("mathDifficulty", mathDifficulty)
        put("mathQuestionCount", mathQuestionCount)
        put("typingCategory", typingCategory)
        val phrasesArray = JSONArray()
        typingCustomPhrases.forEach { phrasesArray.put(it) }
        put("typingCustomPhrases", phrasesArray)
        put("shakeCount", shakeCount)
        val barcodesArray = JSONArray()
        barcodes.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("barcode", item.barcode)
            barcodesArray.put(obj)
        }
        put("barcodes", barcodesArray)
    }
}

fun JSONObject.toWakeUpTask(): WakeUpTask {
    val type = TaskType.valueOf(getString("type"))
    val phrases = mutableListOf<String>()
    val phrasesArray = optJSONArray("typingCustomPhrases")
    if (phrasesArray != null) {
        for (i in 0 until phrasesArray.length()) {
            phrases.add(phrasesArray.getString(i))
        }
    }
    val barcodes = mutableListOf<BarcodeItem>()
    val barcodesArray = optJSONArray("barcodes")
    if (barcodesArray != null) {
        for (i in 0 until barcodesArray.length()) {
            val obj = barcodesArray.getJSONObject(i)
            barcodes.add(
                BarcodeItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    barcode = obj.getString("barcode"),
                )
            )
        }
    }
    return WakeUpTask(
        type = type,
        mathDifficulty = optString("mathDifficulty", MathDifficulty.EASY.name),
        mathQuestionCount = optInt("mathQuestionCount", 3),
        typingCategory = optString("typingCategory", TypingCategory.MOTIVATIONAL.name),
        typingCustomPhrases = phrases,
        shakeCount = optInt("shakeCount", 30),
        barcodes = barcodes,
    )
}

fun List<WakeUpTask>.toJsonString(): String {
    return try {
        val array = JSONArray()
        forEach { array.put(it.toJson()) }
        array.toString()
    } catch (e: Exception) {
        "[]"
    }
}

fun String.toWakeUpTasks(): List<WakeUpTask> {
    return try {
        if (isBlank() || this == "[]") return emptyList()
        val array = JSONArray(this)
        val tasks = mutableListOf<WakeUpTask>()
        for (i in 0 until array.length()) {
            tasks.add(array.getJSONObject(i).toWakeUpTask())
        }
        tasks
    } catch (e: Exception) {
        emptyList()
    }
}

// ─── Display helpers ──────────────────────────────────────────────────────────
fun WakeUpTask.emoji(): String = when (type) {
    TaskType.MATH -> "🧮"
    TaskType.TYPING -> "⌨️"
    TaskType.SHAKE -> "📳"
    TaskType.BARCODE -> "📷"
}

fun WakeUpTask.title(): String = when (type) {
    TaskType.MATH -> "Math ${MathDifficulty.valueOf(mathDifficulty).label}"
    TaskType.TYPING -> "${TypingCategory.valueOf(typingCategory).emoji} Typing"
    TaskType.SHAKE -> "Shake ${shakeCount}×"
    TaskType.BARCODE -> "Scan ${barcodes.size} item${if (barcodes.size != 1) "s" else ""}"
}
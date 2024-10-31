package com.example.alarm_for_student

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

// Function to fetch groups
suspend fun fetchGroups(): List<Group> {
    return withContext(Dispatchers.IO) {
        val groups = mutableListOf<Group>()
        try {
            val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/faculties/").get()
            val elements: Elements = doc.select("a[href^=/gs/faculties/timeline?grp=]")
            for (element in elements) {
                val name = element.text()
                val link = "https://rasp.ssuwt.ru${element.attr("href")}"
                groups.add(Group(name, link))
            }
            Log.d("ScheduleDebug", "парсинг группы")
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении групп: ${e.message}", e)
        }
        groups
    }
}

// Function to fetch schedule
suspend fun fetchSchedule(link: String): List<DaySchedule> {
    return withContext(Dispatchers.IO) {
        val daySchedulesMap = mutableMapOf<String, MutableList<Lesson>>()
        try {
            val doc: Document = Jsoup.connect(link).get()
            val rows: Elements = doc.select("tr")
            for (row in rows) {
                val cells = row.select("td")
                if (cells.isEmpty()) continue
                val time = cells.getOrNull(0)?.text()?.trim() ?: "Нет данных"
                cells.forEachIndexed { index, cell ->
                    val lessonInfo = cell.text().trim()
                    if (lessonInfo.isEmpty()) return@forEachIndexed
                    val day = when (index) {
                        1 -> "Понедельник"
                        2 -> "Вторник"
                        3 -> "Среда"
                        4 -> "Четверг"
                        5 -> "Пятница"
                        6 -> "Суббота"
                        7 -> "Воскресенье"
                        else -> return@forEachIndexed
                    }
                    val lessons = parseLessonInfo(lessonInfo)
                    lessons.forEach { lesson ->
                        val lessonWithTime = lesson.copy(time = time)
                        daySchedulesMap.getOrPut(day) { mutableListOf() }.add(lessonWithTime)
                    }
                }
            }
            Log.d("ScheduleDebug", "парсинг расписание")
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении расписания: ${e.message}", e)
        }
        daySchedulesMap.map { DaySchedule(it.key, it.value) }
    }
}

// Function to parse lesson info
private fun parseLessonInfo(lessonInfo: String): List<Lesson> {
    val regex = Regex(
        """(?<type>лаб|лек|пр)\s+(?<subject>.+?)\s+(?<room>\d+/\w+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.)\s+(подгруппа\s+(?<subgroup>\d+))?"""
    )
    return regex.findAll(lessonInfo).map { match ->
        Lesson(
            time = "",
            type = match.groups["type"]?.value ?: "Неизвестно",
            subject = match.groups["subject"]?.value ?: "Неизвестно",
            room = match.groups["room"]?.value ?: "Неизвестно",
            tutor = match.groups["tutor"]?.value ?: "Неизвестно",
            subgroup = match.groups["subgroup"]?.value
        )
    }.toList()
}

// Function to save schedule to SharedPreferences
fun saveScheduleToPreferences(
    sharedPreferences: SharedPreferences,
    groupName: String,
    daySchedules: List<DaySchedule>,
    gson: Gson
) {
    val daySchedulesJson = gson.toJson(daySchedules)
    sharedPreferences.edit().putString("selectedGroupName", groupName).apply()
    sharedPreferences.edit().putString("daySchedules", daySchedulesJson).apply()
}

// Пример функции для обновления расписания
suspend fun updateSchedule(group: Group, sharedPreferences: SharedPreferences, gson: Gson): List<DaySchedule> {
    // Здесь должна быть логика для обновления расписания
    val updatedSchedules = fetchSchedule(group.link) // Предполагается, что этот метод реализован
    saveScheduleToPreferences(sharedPreferences, group.name, updatedSchedules, gson)
    return updatedSchedules
}

package com.example.alarm_for_student

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

// Функция для получения списка преподавателей с сайта
suspend fun fetchTeachers(): MutableList<Teacher> {
    return withContext(Dispatchers.IO) {
        val teachers = mutableListOf<Teacher>()
        try {
            val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/teachers/").get()
            val tutorElements: Elements = doc.select("a.tutors_item")

            for (element: Element in tutorElements) {
                val name = element.text()
                val link = "https://rasp.ssuwt.ru" + element.attr("href")
                if (name.isEmpty()) continue
                teachers.add(Teacher(name, link))
            }
        } catch (e: Exception) {
            Log.e("TeacherListDebug", "Ошибка при получении списка преподавателей: ${e.message}", e)
        }
        teachers
    }
}

suspend fun fetchWeeksTeacher(scheduleUrl: String): Map<Int, String> {
    return withContext(Dispatchers.IO) {
        val weeksMap = mutableMapOf<Int, String>()
        try {
            val doc: Document = Jsoup.connect(scheduleUrl).get()
            val weekElements: Elements = doc.select("a[href^=?grp=][href*=week=]")
            for (element in weekElements) {
                val weekNumber = element.text().toIntOrNull()
                if (weekNumber != null) {
                    val weekLink = scheduleUrl + element.attr("href")
                    weeksMap[weekNumber] = weekLink
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении недель: ${e.message}", e)
        }
        weeksMap
    }
}

suspend fun fetchTeacherScheduleByDays(link: String): List<DaySchedule> {
    return withContext(Dispatchers.IO) {
        val daySchedulesMap = mutableMapOf<String, MutableList<Lesson>>()
        val dayDateMap = mutableMapOf<String, String>()

        try {
            val doc: Document = Jsoup.connect(link).get()
            val rows: Elements = doc.select("tr")

            val headerRow = doc.select("tr th")
            headerRow.forEach { header ->
                val headerHtml = header.html()
                val parts = headerHtml.split("<br>")
                if (parts.size == 2) {
                    val day = parts[0].trim()
                    val date = parts[1].trim()

                    val dayNumber = date.split("-")[0].trim()

                    when (day) {
                        "Понедельник" -> dayDateMap["Понедельник"] = dayNumber
                        "Вторник" -> dayDateMap["Вторник"] = dayNumber
                        "Среда" -> dayDateMap["Среда"] = dayNumber
                        "Четверг" -> dayDateMap["Четверг"] = dayNumber
                        "Пятница" -> dayDateMap["Пятница"] = dayNumber
                        "Суббота" -> dayDateMap["Суббота"] = dayNumber
                        "Воскресенье" -> dayDateMap["Воскресенье"] = dayNumber
                    }
                }
            }

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
                        else -> return@forEachIndexed
                    }

                    val lessons = parseTeacherLessonInfo(lessonInfo, time)
                    lessons.forEach { lesson ->
                        val lessonWithTime = lesson.copy(time = time)
                        daySchedulesMap.getOrPut(day) { mutableListOf() }.add(lessonWithTime)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при загрузке расписания: ${e.message}", e)
        }

        val allDaysOfWeek = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

        val result = allDaysOfWeek.map { day ->
            val date = dayDateMap[day] ?: "Неизвестно"
            val lessons = daySchedulesMap[day] ?: emptyList()

            DaySchedule(
                day = day,
                date = date,
                lessons = if (lessons.isEmpty()) {
                    listOf(Lesson("Нет данных", "Нет данных", "Нет данных", "Нет данных", "Нет данных", null))
                } else {
                    lessons
                }
            )
        }

        result
    }
}

private fun parseTeacherLessonInfo(lessonInfo: String, time: String): List<Lesson> {
    val regex = Regex(
        """(?<type>лаб|лек|пр|конс|ЭКЗ|зач)\s+(?<subject>.+?)\s+(?<room>\d+/\w+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.)?\s*(подгруппа\s+(?<subgroup>\d+))?"""
    )
    return regex.findAll(lessonInfo).map { match ->
        Lesson(
            time = time,
            type = match.groups["type"]?.value ?: "Неизвестно",
            subject = match.groups["subject"]?.value ?: "Неизвестно",
            room = match.groups["room"]?.value ?: "Неизвестно",
            tutor = match.groups["tutor"]?.value ?: "Неизвестно",
            subgroup = match.groups["subgroup"]?.value
        )
    }.toList()
}

suspend fun fetchTeacherScheduleByWeek(scheduleUrl: String, week: Int): List<DaySchedule> {
    val weekUrl = "$scheduleUrl?week=$week"
    return fetchTeacherScheduleByDays(weekUrl)
}

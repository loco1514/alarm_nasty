package com.example.alarm_for_student

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

// Функция для получения списка преподавателей с сайта
suspend fun fetchTeachers(): List<Teacher> {
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
                Log.d("TeacherListDebug", "Teacher Name: $name, Link: $link")
            }
            Log.d("TeacherListDebug", "Парсинг завершен, найдено преподавателей: ${teachers.size}")
        } catch (e: Exception) {
            Log.e("TeacherListDebug", "Ошибка при получении списка преподавателей: ${e.message}", e)
        }
        teachers
    }
}

// Функция для получения расписания преподавателя
suspend fun fetchTeacherSchedule(link: String): List<Lesson> {
    return withContext(Dispatchers.IO) {
        val lessons = mutableListOf<Lesson>()
        try {
            val doc: Document = Jsoup.connect(link).get()
            val lessonElements: Elements = doc.select(".cell")

            if (lessonElements.isEmpty()) {
                Log.d("TeacherScheduleDebug", "No lesson elements found for link: $link")
                return@withContext lessons // Return empty if no lessons found
            }

            for (element in lessonElements) {
                val type = element.select("span.type").text()
                val subject = element.select("div.subject").text()
                val room = element.select("div.room a").text()
                val subgroup = element.select("div.subg").text()

                if (type.isEmpty() || subject.isEmpty()) {
                    Log.d("TeacherScheduleDebug", "Empty type or subject for element: ${element.html()}")
                    continue // Skip empty entries
                }

                // Логируем всю информацию о уроке
                Log.d("TeacherScheduleDebug", "Lesson found - Type: $type, Subject: $subject, Room: $room, Subgroup: $subgroup")

                lessons.add(
                    Lesson(
                        time = "", // В дальнейшем можно будет добавить время
                        type = type,
                        subject = subject,
                        room = room,
                        tutor = "", // Наполните на основе вашей логики
                        subgroup = subgroup.ifEmpty { null }
                    )
                )
            }
            Log.d("TeacherScheduleDebug", "Fetched ${lessons.size} lessons for link: $link")
        } catch (e: Exception) {
            Log.e("TeacherScheduleDebug", "Error fetching teacher's schedule: ${e.message}", e)
        }
        lessons
    }
}

// Функция для получения расписания преподавателя по дням
suspend fun fetchTeacherScheduleByDays(link: String): List<DaySchedule> {
    return withContext(Dispatchers.IO) {
        val daySchedulesMap = mutableMapOf<String, MutableList<Lesson>>()
        try {
            val doc: Document = Jsoup.connect(link).get()
            val rows: Elements = doc.select("tr")
            Log.d("ScheduleDebug", "Number of rows found: ${rows.size}")

            for (row in rows) {
                val cells = row.select("td")
                if (cells.isEmpty()) continue

                val time = cells.getOrNull(0)?.text()?.trim() ?: "Нет данных"
                Log.d("ScheduleDebug", "Processing time: $time")

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

                    Log.d("ScheduleDebug", "Day: $day, Lesson Info: $lessonInfo")
                    val lessons = parseTeacherLessonInfo(lessonInfo, time)
                    lessons.forEach { lesson ->
                        daySchedulesMap.getOrPut(day) { mutableListOf() }.add(lesson)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении расписания: ${e.message}", e)
        }
        daySchedulesMap.map { DaySchedule(it.key, it.value) }
    }
}

// Функция для парсинга информации о занятии
private fun parseTeacherLessonInfo(lessonInfo: String, time: String): List<Lesson> {
    val regex = Regex(
        """(?<type>лаб|лек|пр)\s+(?<subject>.+?)\s+(?<room>\d+/\w+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.)?\s*(подгруппа\s+(?<subgroup>\d+))?"""
    )
    return regex.findAll(lessonInfo).map { match ->
        Lesson(
            time = time, // Привязываем время к занятию
            type = match.groups["type"]?.value ?: "Неизвестно",
            subject = match.groups["subject"]?.value ?: "Неизвестно",
            room = match.groups["room"]?.value ?: "Неизвестно",
            tutor = match.groups["tutor"]?.value ?: "Неизвестно",
            subgroup = match.groups["subgroup"]?.value
        )
    }.toList()
}

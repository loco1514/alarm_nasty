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
            // Подключение к странице с преподавателями
            val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/teachers/").get()

            // Поиск элементов с классом "tutors_item"
            val tutorElements: Elements = doc.select("a.tutors_item")

            // Извлечение имени и ссылки каждого преподавателя
            for (element: Element in tutorElements) {
                val name = element.text()
                val link = "https://rasp.ssuwt.ru" + element.attr("href")
                if (name.isEmpty()) continue
                teachers.add(Teacher(name, link))
                Log.d("TeacherListDebug", name)
                Log.d("TeacherListDebug", link)
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

            // Парсим расписание из страницы преподавателя
            val lessonElements: Elements = doc.select(".cell")
            for (element in lessonElements) {
                val type = element.select("span.type").text()
                val subject = element.select("div.subject").text()
                val room = element.select("div.room a").text() // Изменяем выбор, чтобы получить текст ссылки
                val subgroup = element.select("div.subg").text()

                // Создаем новый объект Lesson
                lessons.add(
                    Lesson(
                        time = "", // Вы можете настроить это в зависимости от вашей структуры
                        type = type,
                        subject = subject,
                        room = room,
                        tutor = "", // Информация о преподавателе может быть получена из другого источника
                        subgroup = subgroup.ifEmpty { null }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("TeacherScheduleDebug", "Ошибка при получении расписания преподавателя: ${e.message}", e)
        }
        lessons
    }
}
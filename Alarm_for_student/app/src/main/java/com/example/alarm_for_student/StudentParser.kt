package com.example.alarm_for_student

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

suspend fun getCurrentWeek(): Int {
    return withContext(Dispatchers.IO) {
        val url = "https://rasp.ssuwt.ru/gs/faculties/"
        try {
            val document: Document = Jsoup.connect(url).get()

            // Находим элемент с классом "parity" или аналогичный элемент, который содержит информацию о текущей неделе
            val parityElement = document.select("div.parity").firstOrNull()

            // Извлекаем текст из найденного элемента
            val parityText = parityElement?.text() ?: throw IllegalStateException("Не удалось найти элемент с классом 'parity'")

            // Ищем номер недели в тексте, используя регулярное выражение
            val weekNumber = Regex("(\\d+) учебная неделя").find(parityText)?.groupValues?.get(1)
                ?: "1"

            weekNumber.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Повторно выбрасываем исключение, если обработка не требуется
        }
    }
}

private fun parseLessonInfo(lessonInfo: String): List<Lesson> {
    val regex = Regex("""(?<type>лаб|лек|пр|конс|ЭКЗ|зач)\s+(?<subject>.+?)\s+(?<room>\S+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.)(\s+(?<subgroup>\S+))?""")


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

suspend fun fetchSchedule(link: String): List<DaySchedule> {
    return withContext(Dispatchers.IO) {
        val daySchedulesMap = mutableMapOf<String, MutableList<Lesson>>()
        val dayDateMap = mutableMapOf<String, String>() // Map to store day and date pairs

        try {
            val doc: Document = Jsoup.connect(link).get()
            val rows: Elements = doc.select("tr")

            // Находим строки с расписанием
            val headerRow = doc.select("tr th")
            headerRow.forEach { header ->
                val headerHtml = header.html()
                val parts = headerHtml.split("<br>")
                if (parts.size == 2) {
                    val day = parts[0].trim()
                    val date = parts[1].trim()

                    // Извлекаем только число (день месяца) из даты
                    val dayNumber = date.split("-")[0].trim()

                    // Сохраняем день и число
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

            // Обрабатываем строки расписания
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

                    val lessons = parseLessonInfo(lessonInfo)
                    lessons.forEach { lesson ->
                        val lessonWithTime = lesson.copy(time = time)
                        daySchedulesMap.getOrPut(day) { mutableListOf() }.add(lessonWithTime)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при загрузке расписания: ${e.message}", e)
        }

        // Для каждого дня недели создаем объекты DaySchedule
        val allDaysOfWeek = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

        // Создаем список расписаний с числами, даже если уроков нет
        val result = allDaysOfWeek.map { day ->
            val date = dayDateMap[day] ?: "Неизвестно"
            val lessons = daySchedulesMap[day] ?: emptyList()

            DaySchedule(
                day = day,
                date = date, // Даже если пар нет, день будет с числом
                lessons = if (lessons.isEmpty()) {
                    // Если нет уроков, показываем, что нет пар
                    listOf(Lesson("Нет данных", "Нет данных", "Нет данных", "Нет данных", "Нет данных", null))
                } else {
                    lessons
                }
            )
        }

        return@withContext result
    }
}

suspend fun fetchGroups(): Map<String, List<Group>> {
    return withContext(Dispatchers.IO) {
        val groups = mutableListOf<Group>()
        try {
            val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/faculties/").get()

            // Получаем все секции факультетов
            val facultyElements: Elements = doc.select("a[id^=mark-][role=tab]")
            val faculties = mutableMapOf<String, String>()

            // Сопоставляем ID факультетов с их названиями
            for (facultyElement in facultyElements) {
                val facultyId = facultyElement.attr("id")
                val facultyName = facultyElement.text()
                faculties[facultyId] = facultyName
            }

            // Получаем группы для каждого факультета
            for (facultyElement in facultyElements) {
                val facultyId = facultyElement.attr("aria-controls")
                val facultyName = facultyElement.text()

                // Выбираем группы под этим факультетом
                val groupElements: Elements = doc.select("#$facultyId a[href^=/gs/faculties/timeline?grp=]")

                for (groupElement in groupElements) {
                    val name = groupElement.text()
                    val link = "https://rasp.ssuwt.ru${groupElement.attr("href")}"
                    groups.add(Group(name, link, facultyName))
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении групп: ${e.message}", e)
        }

        // Группируем и сортируем группы по факультетам
        groups.groupBy { it.faculty }.mapValues { entry ->
            entry.value.sortedBy { it.name }  // Сортируем группы внутри каждого факультета по алфавиту
        }
    }
}

suspend fun fetchWeeks(scheduleUrl: String): Map<Int, List<DaySchedule>> {
    return withContext(Dispatchers.IO) {
        val weeksMap = mutableMapOf<Int, List<DaySchedule>>()
        val weekNew = getCurrentWeek()
        try {
            val doc: Document = Jsoup.connect(scheduleUrl).get()
            val weekElements: Elements = doc.select("a[href^=?grp=][href*=week=]")
            for (element in weekElements) {
                val weekNumber = element.text().toIntOrNull()
                if (weekNumber != null) {
                    val weekLink = scheduleUrl + element.attr("href")
                    weeksMap[weekNumber] = fetchSchedule(weekLink)
                    if (weekNew - weekNumber == 1){
                        weeksMap[weekNew] = fetchSchedule(scheduleUrl)
                    }
                }
            }
            Log.d("weks", weeksMap[11].toString())
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении недель: ${e.message}", e)
        }
        weeksMap
    }
}
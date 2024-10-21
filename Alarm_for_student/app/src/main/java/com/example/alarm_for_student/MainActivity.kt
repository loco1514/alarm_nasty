package com.example.alarm_for_student

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.alarm_for_student.ui.theme.Alarm_for_studentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.time.LocalDate

data class Lesson(
    val time: String,
    val type: String,
    val subject: String,
    val room: String,
    val tutor: String,
    val subgroup: String? // Поле для подгруппы
)

data class DaySchedule(
    val day: String,
    val lessons: List<Lesson>
)

data class Group(val name: String, val link: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Alarm_for_studentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GroupScheduleScreen()
                }
            }
        }
    }
}

@Composable
fun GroupScheduleScreen() {
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var daySchedules by remember { mutableStateOf(listOf<DaySchedule>()) }
    var showDialog by remember { mutableStateOf(false) }

    // Fetch groups on screen load
    LaunchedEffect(Unit) {
        groups = fetchGroups() // Убедитесь, что это возвращает List<Group>
    }

    // Update schedule when selected group changes
    LaunchedEffect(selectedGroup) {
        selectedGroup?.let {
            daySchedules = fetchSchedule(it.link) // Используйте it.link правильно
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Button to select a group
        TextButton(onClick = { showDialog = true }) {
            Text(text = selectedGroup?.name ?: "Выберите группу", style = MaterialTheme.typography.bodyLarge)
        }

        // AlertDialog for group selection
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Выберите группу") },
                text = {
                    LazyColumn {
                        // Используем items для отображения списка групп
                        items(groups) { group -> // Здесь правильно используйте items
                            ListItem(
                                modifier = Modifier.clickable {
                                    selectedGroup = group // Здесь правильное присвоение
                                    showDialog = false // Закрываем диалог
                                },
                                headlineContent = { Text(text = group.name) } // Доступ к полю name
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display schedule or prompt to select a group
        if (daySchedules.isEmpty()) {
            Text(text = "Выберите группу для отображения расписания.")
        } else {
            ScheduleContent(daySchedules)
        }
    }
}


@Composable
fun ScheduleContent(daySchedules: List<DaySchedule>) {
    val currentDay = LocalDate.now().dayOfWeek.name.lowercase().capitalize()

    val sortedDays = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
    )

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(text = "Расписание:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (daySchedules.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Нет доступного расписания.")
                }
            }
        } else {
            sortedDays.forEach { day ->
                val daySchedule = daySchedules.find { it.day.equals(day, ignoreCase = true) }
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (daySchedule != null && daySchedule.day.equals(currentDay, ignoreCase = true))
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (daySchedule != null && daySchedule.lessons.isNotEmpty()) {
                            val groupedLessons = daySchedule.lessons.groupBy { it.time }
                            groupedLessons.forEach { (time, lessons) ->
                                val lessonsBySubgroup = lessons.groupBy { it.subgroup ?: "Без подгруппы" }
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(text = "Время: $time", style = MaterialTheme.typography.bodyMedium)
                                    lessonsBySubgroup.forEach { (subgroup, subgroupLessons) ->
                                        if (subgroupLessons.isNotEmpty()) {
                                            Text(text = "Подгруппа: $subgroup", style = MaterialTheme.typography.bodyMedium)
                                            subgroupLessons.forEach { lesson ->
                                                LessonRow(lesson)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Уроков нет.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonRow(lesson: Lesson) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = "Дисциплина: ${lesson.subject}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Преподаватель: ${lesson.tutor}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Аудитория: ${lesson.room}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Тип: ${lesson.type}", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

private suspend fun fetchGroups(): List<Group> {
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
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении групп: ${e.message}", e)
        }
        groups
    }
}

private suspend fun fetchSchedule(link: String): List<DaySchedule> {
    return withContext(Dispatchers.IO) {
        val daySchedulesMap = mutableMapOf<String, MutableList<Lesson>>()

        try {
            val doc: Document = Jsoup.connect(link).get()
            val rows: Elements = doc.select("tr")

            for (row in rows) {
                val cells = row.select("td")
                if (cells.isEmpty()) continue
                val time = cells.getOrNull(0)?.text()?.trim() ?: "Нет данных"
                Log.d("ScheduleDebug", "Time: $time")

                cells.forEachIndexed { index, cell ->
                    val lessonInfo = cell.text().trim()
                    Log.d("ScheduleDebug", "Lesson Info: $lessonInfo")
                    if (lessonInfo.isEmpty()) return@forEachIndexed // Пропускаем пустые ячейки
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

                    // Получаем список уроков
                    val lessons = parseLessonInfo(lessonInfo)
                    lessons.forEach { lesson ->
                        val lessonWithTime = lesson.copy(time = time)
                        daySchedulesMap.getOrPut(day) { mutableListOf() }.add(lessonWithTime)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении расписания: ${e.message}", e)
        }

        daySchedulesMap.map { DaySchedule(it.key, it.value) }
    }
}

private fun parseLessonInfo(lessonInfo: String): List<Lesson> {
    Log.d("ScheduleDebug", "Parsing lesson info: $lessonInfo")

    if (lessonInfo.isBlank()) {
        return listOf(
            Lesson(
                time = "Неизвестно",
                type = "Тип урока",
                subject = "Нет данных",
                room = "Нет данных",
                tutor = "Нет данных",
                subgroup = null
            )
        )
    }

    val regex = Regex(
        """(?<type>лаб|лек|пр)\s+(?<subject>.+?)\s+(?<room>\d+/\w+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s[А-ЯЁ]\.\s?[А-ЯЁ]\.)\s*(?<subgroup>\d+\s*п/г)?"""
    )

    val matches = regex.findAll(lessonInfo)

    val lessons = mutableListOf<Lesson>()

    for (match in matches) {
        val type = match.groups["type"]?.value?.trim() ?: "Тип урока"
        val subject = match.groups["subject"]?.value?.trim() ?: "Нет данных"
        val room = match.groups["room"]?.value?.trim() ?: "Нет данных"
        val tutor = match.groups["tutor"]?.value?.trim() ?: "Нет данных"
        val subgroup = match.groups["subgroup"]?.value?.trim()

        Log.d(
            "ScheduleDebug",
            "Parsed subject: $subject, room: $room, tutor: $tutor, subgroup: $subgroup"
        )

        val subgroupValue = subgroup ?: "Без подгруппы"

        lessons.add(
            Lesson(
                time = "Неизвестно",
                type = type,
                subject = subject,
                room = room,
                tutor = tutor,
                subgroup = subgroupValue
            )
        )
    }

    return lessons
}

@Preview(showBackground = true)
@Composable
fun GroupSchedulePreview() {
    Alarm_for_studentTheme {
        GroupScheduleScreen()
    }
}

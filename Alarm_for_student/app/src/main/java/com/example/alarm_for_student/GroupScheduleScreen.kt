package com.example.alarm_for_student

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.lang.reflect.Type
import java.time.LocalDate

@Composable
fun GroupScheduleScreen(sharedPreferences: SharedPreferences) {
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var daySchedules by remember { mutableStateOf(listOf<DaySchedule>()) }
    var showDialog by remember { mutableStateOf(false) }
    var showTutor by remember { mutableStateOf(true) } // Controls tutor visibility
    var showRoom by remember { mutableStateOf(true) }  // Controls room visibility
    val gson = Gson()
    val coroutineScope = rememberCoroutineScope()

    // Load settings for showTutor and showRoom from SharedPreferences (if stored)
    LaunchedEffect(Unit) {
        showTutor = sharedPreferences.getBoolean("showTutor", true)
        showRoom = sharedPreferences.getBoolean("showRoom", true)
        val savedGroupName = sharedPreferences.getString("selectedGroupName", null)
        groups = fetchGroups()

        // Deserialize schedule from JSON
        val daySchedulesJson = sharedPreferences.getString("daySchedules", null)
        val type: Type = object : TypeToken<List<DaySchedule>>() {}.type
        if (daySchedulesJson != null) {
            daySchedules = gson.fromJson(daySchedulesJson, type)
        }

        selectedGroup = groups.find { it.name == savedGroupName }
        if (daySchedules.isEmpty() && selectedGroup != null) {
            daySchedules = fetchSchedule(selectedGroup!!.link)
            saveScheduleToPreferences(sharedPreferences, selectedGroup!!.name, daySchedules, gson)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Button for selecting a group
        TextButton(onClick = { showDialog = true }) {
            Text(text = selectedGroup?.name ?: "Выберите группу", style = MaterialTheme.typography.bodyLarge)
        }

        // Button for updating the schedule
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            selectedGroup?.let { group ->
                coroutineScope.launch {
                    daySchedules = fetchSchedule(group.link)
                    saveScheduleToPreferences(sharedPreferences, group.name, daySchedules, gson)
                }
            }
        }) {
            Text("Обновить расписание")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Выберите группу") },
                text = {
                    LazyColumn {
                        items(groups) { group ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    selectedGroup = group
                                    showDialog = false

                                    // Fetch new schedule when group changes
                                    coroutineScope.launch {
                                        daySchedules = fetchSchedule(group.link)
                                        saveScheduleToPreferences(sharedPreferences, group.name, daySchedules, gson)
                                    }
                                },
                                headlineContent = { Text(text = group.name) }
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

        if (daySchedules.isEmpty()) {
            Text(text = "Выберите группу для отображения расписания.")
        } else {
            ScheduleContent(daySchedules, showTutor, showRoom)
        }
    }
}


@Composable
fun ScheduleContent(daySchedules: List<DaySchedule>, showTutor: Boolean, showRoom: Boolean) {
    val currentDay = LocalDate.now().dayOfWeek.name.lowercase().capitalize()

    val sortedDays = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
    )

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(text = "Расписание:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

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
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(text = "Время: $time", style = MaterialTheme.typography.bodyMedium)
                                lessons.groupBy { it.subgroup ?: "Без подгруппы" }.forEach { (subgroup, subgroupLessons) ->
                                    if (subgroupLessons.isNotEmpty()) {
                                        Text(text = "Подгруппа: $subgroup", style = MaterialTheme.typography.bodyMedium)
                                        subgroupLessons.forEach { lesson ->
                                            LessonRow(lesson, showTutor, showRoom)
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


@Composable
fun LessonRow(lesson: Lesson, showTutor: Boolean, showRoom: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = "Дисциплина: ${lesson.subject}", style = MaterialTheme.typography.bodyMedium)
        if (showTutor) Text(text = "Преподаватель: ${lesson.tutor}", style = MaterialTheme.typography.bodySmall)
        if (showRoom) Text(text = "Аудитория: ${lesson.room}", style = MaterialTheme.typography.bodySmall)
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
            Log.d("ScheduleDebug", "Fetched rows: ${rows.size}")
            for (row in rows) {
                val cells = row.select("td")
                if (cells.isEmpty()) continue
                val time = cells.getOrNull(0)?.text()?.trim() ?: "Нет данных"
                Log.d("ScheduleDebug", "Parsed time: $time")
                cells.forEachIndexed { index, cell ->
                    val lessonInfo = cell.text().trim()
                    Log.d("ScheduleDebug", "Parsed lesson info: $lessonInfo for day index: $index")
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
            Log.d("ScheduleDebug", "Completed schedule parsing: $daySchedulesMap")
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении расписания: ${e.message}", e)
        }
        daySchedulesMap.map { DaySchedule(it.key, it.value) }
    }
}


private fun parseLessonInfo(lessonInfo: String): List<Lesson> {
    val regex = Regex("""(?<type>лаб|лек|пр)\s+(?<subject>.+?)\s+(?<room>\S+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.)(\s+(?<subgroup>\S+))?""")


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

// Функция для сохранения расписания в SharedPreferences
private fun saveScheduleToPreferences(
    sharedPreferences: SharedPreferences,
    groupName: String,
    daySchedules: List<DaySchedule>,
    gson: Gson
) {
    val daySchedulesJson = gson.toJson(daySchedules)
    sharedPreferences.edit().putString("selectedGroupName", groupName).apply()
    sharedPreferences.edit().putString("daySchedules", daySchedulesJson).apply()
}
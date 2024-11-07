package com.example.alarm_for_student

import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GroupScheduleScreen(sharedPreferences: SharedPreferences) {
    var groupedGroups by remember { mutableStateOf(mapOf<String, List<Group>>()) }
    var selectedFaculty by remember { mutableStateOf<String?>(null) }
    var filteredGroups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var daySchedules by remember { mutableStateOf(listOf<DaySchedule>()) }
    var selectedDay by remember { mutableStateOf("Понедельник") } // Default day
    var showDialog by remember { mutableStateOf(false) }
    var showTutor by remember { mutableStateOf(true) } // Controls tutor visibility
    var showRoom by remember { mutableStateOf(true) }  // Controls room visibility
    val gson = Gson()
    val coroutineScope = rememberCoroutineScope()

    // Load settings and initialize groups
    LaunchedEffect(Unit) {
        showTutor = sharedPreferences.getBoolean("showTutor", true)
        showRoom = sharedPreferences.getBoolean("showRoom", true)
        val savedGroupName = sharedPreferences.getString("selectedGroupName", null)
        groupedGroups = fetchGroups()

        val allGroups = groupedGroups.values.flatten()
        selectedGroup = allGroups.find { it.name == savedGroupName }

        if (daySchedules.isEmpty() && selectedGroup != null) {
            daySchedules = fetchSchedule(selectedGroup!!.link)
            saveScheduleToPreferences(sharedPreferences, selectedGroup!!.name, daySchedules, gson)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // Faculty and Group Selection Button
            TextButton(onClick = { showDialog = true }) {
                Text(text = selectedGroup?.name ?: "Выберите группу", style = MaterialTheme.typography.bodyLarge)
            }

            // IconButton for update action
            IconButton(onClick = {
                selectedGroup?.let { group ->
                    coroutineScope.launch {
                        daySchedules = fetchSchedule(group.link)
                        saveScheduleToPreferences(sharedPreferences, group.name, daySchedules, gson)
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить расписание"
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
        DaySelector(
            selectedDay = selectedDay,
            onDayChange = { newDay -> selectedDay = newDay }
        )

        // Group Selection Dialog with Faculty Filter
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Выберите факультет и группу") },
                text = {
                    LazyColumn {
                        // Display faculties first
                        groupedGroups.keys.forEach { faculty ->
                            item {
                                Text(
                                    text = faculty,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            selectedFaculty = faculty
                                            filteredGroups = groupedGroups[faculty] ?: emptyList()
                                        }
                                )
                            }

                            // Display groups for selected faculty
                            if (selectedFaculty == faculty) {
                                items(filteredGroups) { group ->
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

        if (daySchedules.isEmpty()) {
            Text(text = "Выберите группу для отображения расписания.")
        } else {
            // Show selected day's schedule
            val selectedDaySchedule = daySchedules.find { it.day.equals(selectedDay, ignoreCase = true) }
            ScheduleContent(
                daySchedule = selectedDaySchedule,
                showTutor = showTutor,
                showRoom = showRoom
            )
        }
    }
}






@Composable
fun DaySelector(selectedDay: String, onDayChange: (String) -> Unit) {
    val daysOfWeekFull = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"
    )

    val daysOfWeekAbbreviated = listOf(
        "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp), // уменьшенный padding вокруг Row
        horizontalArrangement = Arrangement.spacedBy(2.dp) // уменьшенный отступ между кнопками
    ) {
        daysOfWeekAbbreviated.forEachIndexed { index, abbreviatedDay ->
            val isSelected = daysOfWeekFull[index] == selectedDay
            Button(
                onClick = { onDayChange(daysOfWeekFull[index]) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(6.dp), // уменьшенный padding внутри кнопок
                modifier = Modifier
                    .width(60.dp) // уменьшенная ширина кнопок
                    .shadow(3.dp, RoundedCornerShape(4.dp))
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                Text(
                    text = abbreviatedDay,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize // уменьшенный шрифт текста
                )
            }
        }
    }
}






@Composable
fun ScheduleContent(daySchedule: DaySchedule?, showTutor: Boolean, showRoom: Boolean) {
    if (daySchedule == null || daySchedule.lessons.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Уроков нет.",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            val groupedLessons = daySchedule.lessons.groupBy { it.time }
            groupedLessons.forEach { (time, lessons) ->
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Время
                        Text(
                            text = time,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // Группировка по подгруппам
                        lessons.groupBy { it.subgroup ?: "Без подгруппы" }.forEach { (subgroup, subgroupLessons) ->
                            if (subgroup != "Без подгруппы") {
                                Text(
                                    text = "$subgroup п/г",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            subgroupLessons.forEach { lesson ->
                                LessonRow(lesson, showTutor, showRoom)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun LessonRow(lesson: Lesson, showTutor: Boolean, showRoom: Boolean) {
    // Обертка для урока
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .shadow(4.dp, shape = RoundedCornerShape(12.dp)) // Тень для выделения
            .padding(16.dp)
    ) {
        // Название дисциплины
        Text(
            text = "Дисциплина: ${lesson.subject}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Преподаватель (если включено)
        if (showTutor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Преподаватель",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Преподаватель: ${lesson.tutor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Аудитория (если включено)
        if (showRoom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Аудитория",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Аудитория: ${lesson.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Тип занятия
        Text(
            text = "Тип: ${lesson.type}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp)) // Отступ между уроками
}

private suspend fun fetchGroups(): Map<String, List<Group>> {
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
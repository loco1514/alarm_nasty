package com.example.alarm_for_student

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.alarm_for_student.ui.theme.Alarm_for_studentTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.lang.reflect.Type
import java.time.LocalDate
import androidx.compose.material3.Icon // Import this for Icons
import androidx.compose.material3.IconButton // Ensure this is imported too
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu


data class Lesson(
    val time: String,
    val type: String,
    val subject: String,
    val room: String,
    val tutor: String,
    val subgroup: String?
)

data class DaySchedule(
    val day: String,
    val lessons: List<Lesson>
)

data class Group(val name: String, val link: String)

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        setContent {
            Alarm_for_studentTheme {
                MainScreen(sharedPreferences)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sharedPreferences: SharedPreferences) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope() // Make sure this is defined

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onCloseDrawer = {
                        coroutineScope.launch {
                            drawerState.close() // Use coroutineScope.launch
                        }
                    },
                    onItemSelected = { selectedItem ->
                        when (selectedItem) {
                            "Настройки" -> {
                                // Handle settings navigation
                            }
                            "О приложении" -> {
                                // Handle about navigation
                            }
                        }
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Расписание студентов") },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                GroupScheduleScreen(sharedPreferences)
            }
        }
    }
}




@Composable
fun DrawerContent(onCloseDrawer: () -> Unit, onItemSelected: (String) -> Unit) {
    Column {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        Divider()
        DrawerItem("Расписание преподавателей", onItemSelected, onCloseDrawer)
        DrawerItem("Будильник", onItemSelected, onCloseDrawer)
        DrawerItem("Настройки", onItemSelected, onCloseDrawer)
        DrawerItem("О приложении", onItemSelected, onCloseDrawer)
        // Add more items as needed
    }
}

@Composable
fun DrawerItem(title: String, onItemSelected: (String) -> Unit, onCloseDrawer: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onItemSelected(title)
                onCloseDrawer()
            }
            .padding(16.dp)
    )
}

@Composable
fun GroupScheduleScreen(sharedPreferences: SharedPreferences) {
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var daySchedules by remember { mutableStateOf(listOf<DaySchedule>()) }
    var showDialog by remember { mutableStateOf(false) }
    val gson = Gson()

    // Load saved group and schedule from SharedPreferences
    LaunchedEffect(Unit) {
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

    // Update schedule when selected group changes
    LaunchedEffect(selectedGroup) {
        selectedGroup?.let {
            daySchedules = fetchSchedule(it.link)
            saveScheduleToPreferences(sharedPreferences, it.name, daySchedules, gson)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextButton(onClick = { showDialog = true }) {
            Text(text = selectedGroup?.name ?: "Выберите группу", style = MaterialTheme.typography.bodyLarge)
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
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении расписания: ${e.message}", e)
        }
        daySchedulesMap.map { DaySchedule(it.key, it.value) }
    }
}

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

@Preview(showBackground = true)
@Composable
fun GroupSchedulePreview() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    Alarm_for_studentTheme {
        GroupScheduleScreen(sharedPreferences)
    }
}

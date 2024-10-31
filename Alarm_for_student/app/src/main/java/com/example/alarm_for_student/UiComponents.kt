package com.example.alarm_for_student

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sharedPreferences: SharedPreferences) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showTeacherSchedule by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onItemSelected = { selectedItem ->
                        coroutineScope.launch {
                            when (selectedItem) {
                                "Расписание студентов" -> {
                                    showTeacherSchedule = false
                                }
                                "Расписание преподавателей" -> {
                                    showTeacherSchedule = true
                                }
                                // Handle other menu items if needed
                            }
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
                    title = {
                        Text(if (showTeacherSchedule) "Расписание преподавателей" else "Расписание студентов")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (showTeacherSchedule) {
                    TeacherScheduleScreen(sharedPreferences)
                } else {
                    GroupScheduleScreen(sharedPreferences)
                }
            }
        }
    }
}


@Composable
fun TeacherScheduleScreen(sharedPreferences: SharedPreferences) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Расписание преподавателей", style = MaterialTheme.typography.titleLarge)
        // Add code to display the teacher's schedule
    }
}

@Composable
fun GroupScheduleScreen(sharedPreferences: SharedPreferences) {
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var daySchedules by remember { mutableStateOf(listOf<DaySchedule>()) }
    var showDialog by remember { mutableStateOf(false) }
    val gson = Gson()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedGroupName = sharedPreferences.getString("selectedGroupName", null)
        groups = fetchGroups()
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

    LaunchedEffect(selectedGroup) {
        selectedGroup?.let {
            val savedGroupName = sharedPreferences.getString("selectedGroupName", null)
            if (it.name != savedGroupName) {
                daySchedules = fetchSchedule(it.link)
                saveScheduleToPreferences(sharedPreferences, it.name, daySchedules, gson)
            }
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
        Button(onClick = {
            selectedGroup?.let { group ->
                coroutineScope.launch {
                    daySchedules = updateSchedule(group, sharedPreferences, gson)
                }
            }
        }) {
            Text("Обновить расписание")
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

// Preview for MainScreen
@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    MainScreen(sharedPreferences)
}

package com.example.alarm_for_student

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Place
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeacherScheduleScreen(sharedPreferences: SharedPreferences) {
    var teachers by remember { mutableStateOf(listOf<Teacher>()) }
    var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
    var daySchedules by remember { mutableStateOf(listOf<DaySchedule>()) }
    var selectedDay by remember { mutableStateOf(getCurrentDayOfWeek()) }
    var showDialog by remember { mutableStateOf(false) }
    var showTutor by remember { mutableStateOf(true) }
    var showRoom by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var weeksMap by remember { mutableStateOf(mapOf<Int, List<DaySchedule>>()) }
    var selectedWeek by remember { mutableStateOf(1) } // Инициализация значением по умолчанию

    val gson = Gson()

    // Инициализация расписания и сохраненных данных
    LaunchedEffect(Unit) {
        selectedWeek = withContext(Dispatchers.IO) { getCurrentWeek() } // Вызов в корутине
        teachers = fetchTeachers()
        showTutor = sharedPreferences.getBoolean("showTutor", true)
        showRoom = sharedPreferences.getBoolean("showRoom", true)

        // Загружаем сохраненные данные
        val savedTeacherJson = sharedPreferences.getString("selectedTeacher", null)
        val savedScheduleJson = sharedPreferences.getString("daySchedules", null)
        val savedWeeksMapJson = sharedPreferences.getString("weeksMap", null)

        // Если преподаватель сохранен, загружаем его
        savedTeacherJson?.let {
            selectedTeacher = gson.fromJson(it, Teacher::class.java)
        }

        // Если расписание сохранено, загружаем его
        savedScheduleJson?.let {
            val type = object : TypeToken<List<DaySchedule>>() {}.type
            daySchedules = gson.fromJson(it, type)
        }

        // Если все расписания для недель сохранены, загружаем их
        savedWeeksMapJson?.let {
            val type = object : TypeToken<Map<Int, List<DaySchedule>>>() {}.type
            weeksMap = gson.fromJson(it, type)
            daySchedules = weeksMap[selectedWeek] ?: emptyList()
        }
    }

    LaunchedEffect(selectedTeacher) {
        selectedTeacher?.let { teacher ->
            // После выбора преподавателя, выбираем сегодняшний день
            selectedDay = getCurrentDayOfWeek()
            coroutineScope.launch {
                weeksMap = fetchWeeksTeacher(teacher.link) // Загрузка расписания для всех недель
                daySchedules = weeksMap[selectedWeek] ?: emptyList() // Загружаем расписание для текущей недели
                saveTeacherAndSchedule(sharedPreferences, teacher, weeksMap)
            }
        }
    }

    LaunchedEffect(selectedWeek) {
        // Обновляем расписание для выбранной недели
        daySchedules = weeksMap[selectedWeek] ?: emptyList()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Компонент выбора преподавателя
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            TextButton(onClick = { showDialog = true }) {
                Text(
                    text = selectedTeacher?.name ?: "Выберите преподавателя",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
        val dayDateMap = daySchedules.associate { it.day to it.date }

        // Компонент выбора недели
        WeekSelectorTeacher(
            selectedWeek = selectedWeek,
            onWeekChange = { newWeek ->
                selectedWeek = newWeek
            },
            weeksMap = weeksMap
        )

        // Компонент выбора дня
        DaySelector(
            selectedDay = selectedDay,
            onDayChange = { newDay -> selectedDay = newDay },
            dayDateMap = dayDateMap
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Выберите преподавателя") },
                text = {
                    LazyColumn {
                        items(teachers) { teacher ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    selectedTeacher = teacher
                                    coroutineScope.launch {
                                        selectedWeek = getCurrentWeek() // Инициализация текущей недели
                                        weeksMap = fetchWeeksTeacher(teacher.link) // Загрузка расписания для всех недель
                                        daySchedules = weeksMap[selectedWeek] ?: emptyList() // Загружаем расписание для текущей недели
                                        saveTeacherAndSchedule(sharedPreferences, teacher, weeksMap)
                                    }
                                    showDialog = false
                                },
                                headlineContent = { Text(text = teacher.name) }
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

        if (daySchedules.isEmpty()) {
            Text(text = "Выберите преподавателя для отображения расписания.")
        } else {
            val selectedDaySchedule = daySchedules.find { it.day.equals(selectedDay, ignoreCase = true) }
            ScheduleContentTeacher(daySchedule = selectedDaySchedule)
        }
    }
}

fun saveTeacherAndSchedule(sharedPreferences: SharedPreferences, teacher: Teacher, weeksMap: Map<Int, List<DaySchedule>>) {
    val gson = Gson()
    // Сохраняем преподавателя и расписания для всех недель
    sharedPreferences.edit()
        .putString("selectedTeacher", gson.toJson(teacher))
        .putString("weeksMap", gson.toJson(weeksMap))
        .apply()
}


@Composable
fun WeekSelectorTeacher(
    selectedWeek: Int,
    onWeekChange: (Int) -> Unit,
    weeksMap: Map<Int, List<DaySchedule>>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (selectedWeek > 1) { // Предотвращаем уход ниже первой недели
                    onWeekChange(selectedWeek - 1)
                }
            },
            enabled = selectedWeek > 1 // Отключаем кнопку, если это первая неделя
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Предыдущая неделя"
            )
        }

        Text(
            text = "Неделя: $selectedWeek",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(
            onClick = {
                if (selectedWeek < (weeksMap.keys.maxOrNull() ?: selectedWeek)) { // Ограничиваем до максимальной недели
                    onWeekChange(selectedWeek + 1)
                }
            },
            enabled = selectedWeek < (weeksMap.keys.maxOrNull() ?: selectedWeek) // Отключаем кнопку, если последняя неделя
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Следующая неделя"
            )
        }
    }
}

@Composable
fun ScheduleContentTeacher(daySchedule: DaySchedule?) {
    if (daySchedule == null || daySchedule.lessons.isEmpty() || daySchedule.lessons[0].subject == "Нет данных") {
        // Отображаем сообщение о том, что нет пар
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Пар нет",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            val groupedLessons = daySchedule.lessons.groupBy { it.time }
            groupedLessons.forEach { (time, lessons) ->
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        lessons.groupBy { it.subgroup ?: "Без подгруппы" }.forEach { (subgroup, subgroupLessons) ->
                            if (subgroup != "Без подгруппы") {
                                Text(
                                    text = "$subgroup п/г",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            subgroupLessons.forEach { lesson ->
                                LessonRowTeacher(lesson)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonRowTeacher(lesson: Lesson) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Дисциплина: ${lesson.subject}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

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

        Text(
            text = "Тип: ${lesson.type}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

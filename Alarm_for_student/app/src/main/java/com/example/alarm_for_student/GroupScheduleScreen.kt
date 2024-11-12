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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GroupScheduleScreen(
    sharedPreferences: SharedPreferences,
    initialDaySchedules: List<DaySchedule>,
) {
    var groupedGroups by remember { mutableStateOf(mapOf<String, List<Group>>()) }
    var selectedFaculty by remember { mutableStateOf<String?>(null) }
    var filteredGroups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedDay by remember { mutableStateOf(getCurrentDayOfWeek()) }
    var selectedWeek by remember { mutableStateOf(1) } // Неделя по умолчанию - 1
    var weeksMap by remember { mutableStateOf(mapOf<Int, List<DaySchedule>>()) }
    var showDialog by remember { mutableStateOf(false) }
    var showTutor by remember { mutableStateOf(true) }
    var showRoom by remember { mutableStateOf(true) }
    val gson = Gson()
    val coroutineScope = rememberCoroutineScope()
    var daySchedules by remember { mutableStateOf(initialDaySchedules) }

    // Загрузка сохранённых значений из SharedPreferences
    LaunchedEffect(Unit) {
        selectedWeek = sharedPreferences.getInt("selectedWeek", 1) // Неделя по умолчанию - 1
        showTutor = sharedPreferences.getBoolean("showTutor", true)
        showRoom = sharedPreferences.getBoolean("showRoom", true)

        val savedGroupName = sharedPreferences.getString("selectedGroupName", null) ?: ""
        groupedGroups = fetchGroups()
        selectedGroup = findGroupInShedule(savedGroupName, groupedGroups)

        // Загружаем сохранённые данные по неделям, если они есть
        val savedWeeksJson = sharedPreferences.getString("weeksMap_${selectedGroup?.name}", null)
        if (savedWeeksJson != null) {
            weeksMap = gson.fromJson(savedWeeksJson, object : TypeToken<Map<Int, List<DaySchedule>>>() {}.type)
        } else if (selectedGroup != null) {
            // Загружаем данные по неделям только если их нет в памяти
            weeksMap = fetchWeeks(selectedGroup!!.link)
            saveAllWeeksToPreferences(sharedPreferences, selectedGroup!!.name, weeksMap, gson)
        }
    }

    // При изменении выбранной группы, загружаем все недели для этой группы
    LaunchedEffect(selectedGroup) {
        selectedGroup?.let { group ->
            val savedWeeksJson = sharedPreferences.getString("weeksMap_${group.name}", null)
            if (savedWeeksJson != null) {
                weeksMap = gson.fromJson(savedWeeksJson, object : TypeToken<Map<Int, List<DaySchedule>>>() {}.type)
            } else {
                weeksMap = fetchWeeks(group.link)
                saveAllWeeksToPreferences(sharedPreferences, group.name, weeksMap, gson)
            }

            // Если уже выбрана неделя, загружаем её расписание
            daySchedules = weeksMap[selectedWeek] ?: emptyList()
        }
    }

    // При изменении недели обновляем расписание для выбранной недели
    LaunchedEffect(selectedWeek) {
        daySchedules = weeksMap[selectedWeek] ?: emptyList()
    }

    val currentDate = getCurrentDate()
    val isEven = isEvenWeek()

    Column(modifier = Modifier.padding(16.dp)) {
        // Заголовок с датой и неделей
        Text(
            text = "Сегодня: $currentDate",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Неделя: ${if (isEven) "Четная" else "Нечетная"}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Кнопка выбора группы с иконкой
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Group, contentDescription = "Group Icon")
                Text(
                    text = selectedGroup?.name ?: "Выберите группу",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Селектор недели
        WeekSelector(
            selectedWeek = selectedWeek,
            onWeekChange = { newWeek ->
                selectedWeek = newWeek
                sharedPreferences.edit().putInt("selectedWeek", newWeek).apply() // Сохраняем выбранную неделю
                coroutineScope.launch {
                    daySchedules = weeksMap[newWeek] ?: emptyList()
                    saveScheduleToPreferences(sharedPreferences, selectedGroup!!.name, daySchedules, gson)
                }
            },
            weeksMap = weeksMap
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Селектор дня с отображением даты
        val dayDateMap = daySchedules.associate { it.day to it.date }
        DaySelector(
            selectedDay = selectedDay,
            onDayChange = { newDay -> selectedDay = newDay },
            dayDateMap = dayDateMap
        )

        // Диалог для выбора группы и факультета
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Выберите факультет и группу", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn {
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
                            if (selectedFaculty == faculty) {
                                items(filteredGroups) { group ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            selectedGroup = group
                                            showDialog = false
                                            coroutineScope.launch {
                                                daySchedules = fetchSchedule(group.link)
                                                saveScheduleToPreferences(
                                                    sharedPreferences,
                                                    group.name,
                                                    daySchedules,
                                                    gson
                                                )
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

        // Отображение расписания для выбранного дня
        if (daySchedules.isEmpty()) {
            Text(text = "Выберите группу для отображения расписания.")
        } else {
            val selectedDaySchedule = daySchedules.find { it.day.equals(selectedDay, ignoreCase = true) }
            ScheduleContent(
                daySchedule = selectedDaySchedule,
                showTutor = showTutor,
                showRoom = showRoom
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentDayOfWeek(): String {
    return when (LocalDate.now().dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "Понедельник"
        java.time.DayOfWeek.TUESDAY -> "Вторник"
        java.time.DayOfWeek.WEDNESDAY -> "Среда"
        java.time.DayOfWeek.THURSDAY -> "Четверг"
        java.time.DayOfWeek.FRIDAY -> "Пятница"
        java.time.DayOfWeek.SATURDAY -> "Суббота"
        else -> "Воскресенье" // Воскресенье, если вдруг будет важно
    }
}

fun findGroupInShedule(
    findGroup: String,
    groupedGroups: Map<String, List<Group>>
): Group? {
    // Загружаем данные о группах
    Log.d("GroupScheduleScreen", "Список групп из fetchGroups: ${groupedGroups.keys}") // Лог для диагностики

    // Ищем группу по названию
    val allGroups = groupedGroups.values.flatten()
    val selectedGroup = allGroups.find { it.name == findGroup }

    Log.d("GroupScheduleScreen", "Найденная группа: ${selectedGroup?.name ?: "Не найдена"}") // Лог для диагностики
    return selectedGroup
}

@Composable
fun WeekSelector(selectedWeek: Int, onWeekChange: (Int) -> Unit, weeksMap: Map<Int, List<DaySchedule>>) {
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Следующая неделя"
            )
        }
    }
}


@Composable
fun DaySelector(selectedDay: String, onDayChange: (String) -> Unit, dayDateMap: Map<String, String>) {
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
            val date = dayDateMap[daysOfWeekFull[index]] ?: "Неизвестно"  // теперь будет число

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onDayChange(daysOfWeekFull[index]) }
            ) {
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
                    // Используем Column с центрированием контента
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), // уменьшенный вертикальный padding
                        horizontalAlignment = Alignment.CenterHorizontally, // Центрируем содержимое по горизонтали
                        verticalArrangement = Arrangement.Center // Центрируем содержимое по вертикали
                    ) {
                        Text(
                            text = abbreviatedDay,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize // уменьшенный шрифт текста
                        )
                        Text(
                            text = date, // теперь отображаем только число
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentDate(): String {
    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))
    return currentDate.format(formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun isEvenWeek(): Boolean {
    val currentDate = LocalDate.now()
    val weekOfYear = currentDate.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    return weekOfYear % 2 == 0
}

@Composable
fun ScheduleContent(daySchedule: DaySchedule?, showTutor: Boolean, showRoom: Boolean) {
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

// Функция для сохранения расписания в SharedPreferences
fun saveScheduleToPreferences(
    sharedPreferences: SharedPreferences,
    groupName: String,
    daySchedules: List<DaySchedule>,
    gson: Gson
) {
    val editor = sharedPreferences.edit()
    val scheduleJson = gson.toJson(daySchedules)
    editor.putString("daySchedules", scheduleJson)
    editor.putString("selectedGroupName", groupName)
    editor.apply()
}

// Функция для сохранения всех недель расписания в SharedPreferences
fun saveAllWeeksToPreferences(
    sharedPreferences: SharedPreferences,
    groupName: String,
    weeksMap: Map<Int, List<DaySchedule>>,
    gson: Gson
) {
    val editor = sharedPreferences.edit()
    val weeksJson = gson.toJson(weeksMap)
    editor.putString("weeksMap_$groupName", weeksJson)
    editor.apply()
}


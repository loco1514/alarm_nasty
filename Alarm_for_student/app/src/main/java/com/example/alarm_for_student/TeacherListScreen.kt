package com.example.alarm_for_student

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherListScreen(sharedPreferences: SharedPreferences) {
    var teachers by remember { mutableStateOf<List<Teacher>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
    var selectedTeacherSchedule by remember { mutableStateOf<List<DaySchedule>?>(null) }
    var history by remember { mutableStateOf<List<Pair<Teacher, List<DaySchedule>>>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Загрузка последнего выбора и истории при запуске
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            loadLastSelection(sharedPreferences)?.let { (lastTeacher, lastSchedule) ->
                selectedTeacher = lastTeacher
                selectedTeacherSchedule = lastSchedule
                Log.d("TeacherListScreen", "Loaded last selection: ${lastTeacher.name}")
            } ?: Log.d("TeacherListScreen", "No previous selection found.")

            history = loadSelectionHistory(sharedPreferences)
            teachers = fetchTeachers().sortedBy { it.name }
            isLoading = false
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Выберите преподавателя", style = MaterialTheme.typography.titleLarge)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            var expanded by remember { mutableStateOf(false) }

            TextField(
                readOnly = true,
                value = selectedTeacher?.name ?: "Выберите преподавателя",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                colors = TextFieldDefaults.colors()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                teachers.forEach { teacher ->
                    DropdownMenuItem(
                        text = { Text(teacher.name) },
                        onClick = {
                            selectedTeacher = teacher
                            expanded = false

                            coroutineScope.launch {
                                selectedTeacherSchedule = fetchTeacherScheduleByDays(teacher.link)
                                saveLastSelection(sharedPreferences, teacher, selectedTeacherSchedule)
                                saveSelectionHistory(sharedPreferences, teacher, selectedTeacherSchedule)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            selectedTeacherSchedule?.let { schedule ->
                Text(
                    text = "Расписание для ${selectedTeacher?.name}:",
                    style = MaterialTheme.typography.titleMedium
                )
                ScheduleByDayContent(schedule)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("История выборов", style = MaterialTheme.typography.titleMedium)
            history.forEach { (teacher, schedule) ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Преподаватель: ${teacher.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                ScheduleByDayContent(schedule)
            }
        }
    }
}

// Функция для сохранения последнего выбора
// Обновленная функция для загрузки последнего выбора
private fun loadLastSelection(sharedPreferences: SharedPreferences): Pair<Teacher, List<DaySchedule>>? {
    val gson = Gson()
    val teacherJson = sharedPreferences.getString("lastSelectedTeacher", null)
    val scheduleJson = sharedPreferences.getString("lastSelectedSchedule", null)

    if (teacherJson != null && scheduleJson != null) {
        val teacher = gson.fromJson(teacherJson, Teacher::class.java)
        val scheduleType = object : TypeToken<List<DaySchedule>>() {}.type
        val schedule: List<DaySchedule> = gson.fromJson(scheduleJson, scheduleType)
        return teacher to schedule
    }
    return null
}

// Обновленная функция для загрузки истории выборов
// Функция для загрузки истории выборов с использованием TypeToken для конкретного типа
private fun loadSelectionHistory(sharedPreferences: SharedPreferences): List<Pair<Teacher, List<DaySchedule>>> {
    val gson = Gson()
    val historyJson = sharedPreferences.getString("selectionHistory", null)

    if (historyJson != null) {
        // Создаем тип для корректной десериализации списка пар
        val historyType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val rawHistory: List<Map<String, Any>> = gson.fromJson(historyJson, historyType)

        // Преобразуем каждый элемент из LinkedTreeMap в объекты Teacher и DaySchedule
        return rawHistory.mapNotNull { entry ->
            val teacher = gson.fromJson(gson.toJson(entry["first"]), Teacher::class.java)
            val scheduleJson = gson.toJson(entry["second"])
            val scheduleType = object : TypeToken<List<DaySchedule>>() {}.type
            val schedule = gson.fromJson<List<DaySchedule>>(scheduleJson, scheduleType)
            teacher to schedule
        }
    }
    return emptyList()
}

// Функция для сохранения истории выборов
private fun saveSelectionHistory(
    sharedPreferences: SharedPreferences,
    teacher: Teacher,
    schedule: List<DaySchedule>?
) {
    val gson = Gson()
    val historyJson = sharedPreferences.getString("selectionHistory", null)

    // Загружаем существующую историю и добавляем новый выбор
    val historyType = object : TypeToken<List<Pair<Teacher, List<DaySchedule>>>>() {}.type
    val existingHistory: MutableList<Pair<Teacher, List<DaySchedule>>> = if (historyJson != null) {
        gson.fromJson(historyJson, historyType)
    } else {
        mutableListOf()
    }

    // Добавляем новый выбор в историю
    existingHistory.add(teacher to (schedule ?: emptyList()))
    val editor = sharedPreferences.edit()
    editor.putString("selectionHistory", gson.toJson(existingHistory))
    editor.apply()

    Log.d("TeacherListScreen", "Added to selection history: ${teacher.name}")
}


// Функция для сохранения последнего выбора преподавателя и расписания в SharedPreferences
private fun saveLastSelection(sharedPreferences: SharedPreferences, teacher: Teacher, schedule: List<DaySchedule>?) {
    val editor = sharedPreferences.edit()
    val gson = Gson()

    // Сохранение преподавателя и расписания в формате JSON
    editor.putString("lastSelectedTeacher", gson.toJson(teacher))
    editor.putString("lastSelectedSchedule", gson.toJson(schedule))
    editor.apply()

    Log.d("TeacherListScreen", "Saved last selection: ${teacher.name}")
}

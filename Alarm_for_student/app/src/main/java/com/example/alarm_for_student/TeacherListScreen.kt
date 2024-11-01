package com.example.alarm_for_student

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TeacherListScreen() {
    var teachers by remember { mutableStateOf<List<Teacher>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTeacherSchedule by remember { mutableStateOf<List<Lesson>?>(null) } // State for selected teacher's schedule
    val coroutineScope = rememberCoroutineScope()

    // Fetch teachers when this composable is first launched
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                teachers = fetchTeachers() // Fetch teachers asynchronously
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Не удалось загрузить список преподавателей: ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Список преподавателей", style = MaterialTheme.typography.titleLarge)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        } else {
            val groupedTeachers = teachers.groupBy { it.name.first().uppercase() }
            LazyColumn {
                items(groupedTeachers.toList()) { (initial, teachersGroup) ->
                    GroupedTeacherGroup(initial, teachersGroup) { teacher ->
                        // Fetch schedule for the selected teacher
                        coroutineScope.launch {
                            selectedTeacherSchedule = fetchTeacherSchedule(teacher.link)
                        }
                    }
                }
            }
        }

        // Display the selected teacher's schedule if available
        selectedTeacherSchedule?.let { schedule ->
            ScheduleContent(schedule) // Create a ScheduleContent composable to display the schedule
        }
    }
}

@Composable
fun GroupedTeacherGroup(initial: String, teachersGroup: List<Teacher>, onTeacherSelected: (Teacher) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val headerModifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = !expanded }
        .padding(16.dp)

    Column {
        Text(text = initial, style = MaterialTheme.typography.titleMedium, modifier = headerModifier)

        if (expanded) {
            Column {
                for (teacher in teachersGroup) {
                    ListItem(
                        modifier = Modifier.clickable {
                            onTeacherSelected(teacher) // Notify parent composable to fetch schedule
                        },
                        headlineContent = { Text(text = teacher.name) },
                        trailingContent = { Text(text = "Ссылка") }
                    )
                }
            }
        }
    }
}
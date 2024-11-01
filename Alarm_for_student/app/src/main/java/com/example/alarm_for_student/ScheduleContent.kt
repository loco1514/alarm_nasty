package com.example.alarm_for_student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// This should be the only definition of LessonRow
@Composable
fun LessonRowTeacher(lesson: Lesson) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = "Дисциплина: ${lesson.subject}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Тип: ${lesson.type}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Аудитория: ${lesson.room}", style = MaterialTheme.typography.bodySmall)
        lesson.subgroup?.let { Text(text = "Подгруппа: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun ScheduleContent(lessons: List<Lesson>) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(lessons) { lesson ->
            LessonRowTeacher(lesson) // Call the LessonRow function
        }
    }
}
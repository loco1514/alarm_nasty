package com.example.alarm_for_student

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.alarm_for_student.ui.theme.Alarm_for_studentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

data class Lesson(
    val time: String,
    val type: String,
    val subject: String,
    val room: String,
    val tutor: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Alarm_for_studentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScheduleScreen()
                }
            }
        }
    }

    @Composable
    fun ScheduleScreen() {
        var lessons by remember { mutableStateOf(listOf<Lesson>()) } // Mutable state for lessons

        // Launch coroutine to fetch the schedule when the screen is displayed
        LaunchedEffect(Unit) {
            lessons = fetchSchedule()
        }

        // Display the schedule in the UI as a table
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Schedule:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (lessons.isEmpty()) {
                Text(text = "No schedule available.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn {
                    items(lessons) { lesson ->
                        LessonRow(lesson)
                    }
                }
            }
        }
    }

    @Composable
    fun LessonRow(lesson: Lesson) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) { // Убрали .border(1.dp, MaterialTheme.colorScheme.onSurface)
            // Display each lesson's details
            Text(
                text = lesson.time,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = lesson.type,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = lesson.subject,
                modifier = Modifier
                    .weight(3f)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = lesson.room,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = lesson.tutor,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    private suspend fun fetchSchedule(): List<Lesson> {
        return withContext(Dispatchers.IO) {
            val lessonsList = mutableListOf<Lesson>()
            try {
                // Fetch the document from the URL
                val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/faculties/timeline?grp=17039").get()

                // Select all rows of the schedule table
                val rows: Elements = doc.select("tr") // Selecting all <tr> elements

                // Iterate through each row and extract relevant data
                for (row in rows) {
                    val cells = row.select("td") // Select all <td> elements in the row

                    // Extract time from the first cell
                    if (cells.isNotEmpty()) {
                        val time = cells[0].text() // e.g., "3-я пара\n12:30-14:05"

                        // Iterate through remaining cells to extract subjects and details
                        for (cell in cells.subList(1, cells.size)) {
                            val lessonType = cell.select(".type").text() // e.g., "лаб" or "лек"
                            val subject = cell.select(".subject").text() // Subject name
                            val room = cell.select(".room a").text() // Room number
                            val tutor = cell.select(".tutor").text() // Tutor name

                            // Add lesson to the list if valid information is found
                            if (lessonType.isNotEmpty() && subject.isNotEmpty()) {
                                lessonsList.add(Lesson(time, lessonType, subject, room, tutor))
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                // Handle exceptions (network issues, parsing errors, etc.)
                Log.e("ScheduleDebug", "Error fetching schedule: ${e.message}")
            }

            lessonsList // Return the list of lessons
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun SchedulePreview() {
        Alarm_for_studentTheme {
            // Example preview with mock data
            ScheduleScreen()
        }
    }
}

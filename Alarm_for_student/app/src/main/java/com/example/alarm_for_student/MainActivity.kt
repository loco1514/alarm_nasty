package com.example.alarm_for_student

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.alarm_for_student.ui.theme.Alarm_for_studentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

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
        var schedule by remember { mutableStateOf("") } // Mutable state for schedule

        // Launch coroutine to fetch the schedule when the screen is displayed
        LaunchedEffect(Unit) {
            schedule = fetchSchedule()
        }

        // Display the schedule in the UI
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Schedule:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = schedule.ifEmpty { "No schedule available." }) // Display message if no schedule
        }
    }

    private suspend fun fetchSchedule(): String {
        return withContext(Dispatchers.IO) {
            val scheduleData = StringBuilder()
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
                        scheduleData.append("Time: $time\n")

                        // Iterate through remaining cells to extract subjects and details
                        for (cell in cells.subList(1, cells.size)) {
                            val lessonType = cell.select(".type").text() // e.g., "лаб" or "лек"
                            val subject = cell.select(".subject").text() // Subject name
                            val room = cell.select(".room a").text() // Room number
                            val tutor = cell.select(".tutor").text() // Tutor name

                            // Append formatted lesson information
                            if (lessonType.isNotEmpty() && subject.isNotEmpty()) {
                                scheduleData.append("$lessonType: $subject\nRoom: $room\nTutor: $tutor\n\n")
                            }
                        }
                        scheduleData.append("\n") // Add extra space between rows
                    }
                }

            } catch (e: Exception) {
                // Handle exceptions (network issues, parsing errors, etc.)
                Log.e("ScheduleDebug", "Error fetching schedule: ${e.message}")
                return@withContext "Error fetching schedule: ${e.message}"
            }

            scheduleData.toString()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        Alarm_for_studentTheme {
            ScheduleScreen()
        }
    }
}
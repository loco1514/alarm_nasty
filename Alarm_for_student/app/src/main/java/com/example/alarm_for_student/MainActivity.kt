package com.example.alarm_for_student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.alarm_for_student.ui.theme.Alarm_for_studentTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

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
        val schedule = remember { mutableStateOf("") }

        CoroutineScope(Dispatchers.IO).launch {
            schedule.value = fetchSchedule()
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Schedule:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = schedule.value)
        }
    }

    private suspend fun fetchSchedule(): String {
        return withContext(Dispatchers.IO) {
            val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/faculties/timeline?grp=17039").get()
            val scheduleData = StringBuilder()

            // Replace with the actual selectors for the schedule items on the page
            val elements = doc.select("YOUR_CSS_SELECTOR") // Update this to select the correct HTML elements

            for (element in elements) {
                scheduleData.append(element.text()).append("\n")
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

package com.example.alarm_for_student

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(sharedPreferences: SharedPreferences) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Настройки", style = MaterialTheme.typography.titleLarge)

        // Load saved preferences
        var showTuror by remember { mutableStateOf(sharedPreferences.getBoolean("showTutor", true)) }
        var showRoom by remember { mutableStateOf(sharedPreferences.getBoolean("showRoom", true)) }
        var showSubgroup by remember { mutableStateOf(sharedPreferences.getBoolean("showSubgroup", true)) }

        // Checkbox for showing/hiding Tutor
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Показывать преподавателя", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = showTuror,
                onCheckedChange = {
                    showTuror = it
                    saveSetting(sharedPreferences, "showTutor", showTuror)
                }
            )
        }

        // Checkbox for showing/hiding Room
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Показывать корпус", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = showRoom,
                onCheckedChange = {
                    showRoom = it
                    saveSetting(sharedPreferences, "showRoom", showRoom)
                }
            )
        }

        // Checkbox for showing/hiding Subgroup
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Показывать подгруппу", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = showSubgroup,
                onCheckedChange = {
                    showSubgroup = it
                    saveSetting(sharedPreferences, "showSubgroup", showSubgroup)
                }
            )
        }

        // More settings can be added here
    }
}

// Function to save settings to SharedPreferences
private fun saveSetting(sharedPreferences: SharedPreferences, key: String, value: Boolean) {
    val editor = sharedPreferences.edit()
    editor.putBoolean(key, value)
    editor.apply()
}

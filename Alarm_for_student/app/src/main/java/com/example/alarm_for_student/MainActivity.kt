package com.example.alarm_for_student

import android.os.Bundle
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        setContent {
            MaterialTheme {
                // Передаем sharedPreferences в AppContent
                AppContent(sharedPreferences)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppContent(sharedPreferences: SharedPreferences) {
    //var isLoggedIn by remember { mutableStateOf(AuthUtils.isUserLoggedIn()) }

    MainScreen(sharedPreferences)

    /*if (isLoggedIn) {
        // Provide the required parameters for ScheduleContent
        val daySchedules = remember { listOf<DaySchedule>() } // Замените на фактические данные
        val showTutor = true // Установите на основе вашей логики
        val showRoom = true // Установите на основе вашей логики

        // Показываем основной контент после входа
        MainScreen(sharedPreferences)
    } else {
        // Показываем экран входа
        var isRegistering by remember { mutableStateOf(false) }

        if (isRegistering) {
            RegisterScreen {
                isLoggedIn = true // Предполагая, что регистрация прошла успешно
            }
        } else {
            LoginScreen(
                onLoginSuccess = { isLoggedIn = true },
                onNavigateToRegister = { isRegistering = true }
            )
        }
    }*/
}

package com.example.alarm_for_student

import android.app.Application
import com.google.firebase.FirebaseApp
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "Firebase успешно инициализирован")
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Ошибка инициализации Firebase", e)
        }
    }
}

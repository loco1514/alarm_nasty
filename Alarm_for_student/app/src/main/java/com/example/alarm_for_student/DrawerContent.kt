package com.example.alarm_for_student

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DrawerContent(onCloseDrawer: () -> Unit, onItemSelected: (String) -> Unit) {
    Column {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        Divider()
        DrawerItem("Расписание студентов", onItemSelected, onCloseDrawer)
        DrawerItem("Расписание преподавателей", onItemSelected, onCloseDrawer)
        DrawerItem("Будильник", onItemSelected, onCloseDrawer)
        DrawerItem("Настройки", onItemSelected, onCloseDrawer)
        DrawerItem("О приложении", onItemSelected, onCloseDrawer)
    }
}

@Composable
fun DrawerItem(title: String, onItemSelected: (String) -> Unit, onCloseDrawer: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d("DrawerItem", "Нажатие на элемент меню: $title")
                onItemSelected(title)
                onCloseDrawer()
            }
            .padding(16.dp)
    )
}

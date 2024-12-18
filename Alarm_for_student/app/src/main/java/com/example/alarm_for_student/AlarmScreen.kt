package com.example.alarm_for_student

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

data class Alarm(
    val time: String,
    var isEnabled: Boolean = true,
    var repeatDays: Set<String> = emptySet(),
    var repeatMode: RepeatMode = RepeatMode.NONE
)

enum class RepeatMode {
    NONE,
    WEEKLY,
    EVEN_WEEKS,
    ODD_WEEKS
}

enum class WeekDays {
    ПН, ВТ, СР, ЧТ, ПТ, СБ, ВС
}

@Composable
fun AlarmScreen() {
    var alarms by remember { mutableStateOf(listOf<Alarm>()) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var selectedAlarm by remember { mutableStateOf<Alarm?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Будильники", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { selectedAlarm = null; showAlarmDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить будильник", tint = MaterialTheme.colorScheme.primary)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(alarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onToggle = { isEnabled ->
                        alarms = alarms.map {
                            if (it == alarm) it.copy(isEnabled = isEnabled) else it
                        }
                        if (isEnabled) {
                            scheduleAlarm(context, alarm)
                        } else {
                            cancelAlarm(context, alarm)
                        }
                    },
                    onEdit = { selectedAlarm = alarm; showAlarmDialog = true },
                    onDelete = {
                        cancelAlarm(context, alarm)
                        alarms = alarms.filter { it != alarm }
                    }
                )
            }
        }
    }

    if (showAlarmDialog) {
        ShowAlarmDialog(
            context = context,
            initialAlarm = selectedAlarm,
            onAlarmSaved = { alarm ->
                alarms = alarms.filter { it != selectedAlarm } + alarm
                scheduleAlarm(context, alarm)
                showAlarmDialog = false
            },
            onDismiss = { showAlarmDialog = false }
        )
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onEdit() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Время отдельно слева
        Text(
            text = alarm.time,
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 16.dp)
        )

        // Информация о повторении в колонке
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Повтор: ${when (alarm.repeatMode) {
                    RepeatMode.NONE -> "Нет"
                    RepeatMode.WEEKLY -> "Каждую неделю"
                    RepeatMode.EVEN_WEEKS -> "Четные недели"
                    RepeatMode.ODD_WEEKS -> "Нечетные недели"
                }}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (alarm.repeatMode != RepeatMode.NONE && alarm.repeatDays.isNotEmpty()) {
                Text(
                    text = "${alarm.repeatDays.joinToString(", ")}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Переключатель и кнопка удаления
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить будильник",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAlarmDialog(
    context: Context,
    initialAlarm: Alarm?,
    onAlarmSaved: (Alarm) -> Unit,
    onDismiss: () -> Unit
) {
    var time by remember { mutableStateOf(initialAlarm?.time ?: "07:00") }
    var repeatMode by remember { mutableStateOf(initialAlarm?.repeatMode ?: RepeatMode.NONE) }
    var selectedDays by remember { mutableStateOf(initialAlarm?.repeatDays ?: emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Настройка будильника") },
        text = {
            Column {
                // Time Picker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Время: ", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        val calendar = Calendar.getInstance()
                        val timePickerDialog = TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                time = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        )
                        timePickerDialog.show()
                    }) {
                        Text(text = time, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat mode selection
                Text(text = "Режим повторения:", style = MaterialTheme.typography.bodyLarge)
                RepeatMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { repeatMode = mode }
                    ) {
                        RadioButton(selected = (repeatMode == mode), onClick = { repeatMode = mode })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = when (mode) {
                            RepeatMode.NONE -> "Нет"
                            RepeatMode.WEEKLY -> "Каждую неделю"
                            RepeatMode.EVEN_WEEKS -> "Четные недели"
                            RepeatMode.ODD_WEEKS -> "Нечетные недели"
                        })
                    }
                }

                // Days of week selection
                if (repeatMode != RepeatMode.NONE) {
                    Text(text = "Выберите дни недели:", style = MaterialTheme.typography.bodyLarge)
                    WeekDays.values().forEach { day ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedDays = if (selectedDays.contains(day.name)) {
                                    selectedDays - day.name
                                } else {
                                    selectedDays + day.name
                                }
                            }
                        ) {
                            Checkbox(checked = selectedDays.contains(day.name), onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = day.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAlarmSaved(
                    Alarm(
                        time = time,
                        repeatMode = repeatMode,
                        repeatDays = selectedDays
                    )
                )
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val (hour, minute) = alarm.time.split(":").map { it.toInt() }

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)

        // Adjust for repeat mode
        when (alarm.repeatMode) {
            RepeatMode.EVEN_WEEKS -> {
                if (get(Calendar.WEEK_OF_YEAR) % 2 != 0) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RepeatMode.ODD_WEEKS -> {
                if (get(Calendar.WEEK_OF_YEAR) % 2 == 0) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            else -> {}
        }

        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarmTime", alarm.time)
        putExtra("repeatMode", alarm.repeatMode.name)
        putExtra("repeatDays", alarm.repeatDays.toTypedArray())
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.time.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    when (alarm.repeatMode) {
        RepeatMode.NONE -> {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
        else -> {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }
}

fun cancelAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarmTime", alarm.time)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.time.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmTime = intent.getStringExtra("alarmTime")
        val repeatMode = intent.getStringExtra("repeatMode")?.let { RepeatMode.valueOf(it) } ?: RepeatMode.NONE
        val repeatDays = intent.getStringArrayExtra("repeatDays")?.toSet() ?: emptySet()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL",
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            Notification.Builder(context, "ALARM_CHANNEL")
                .setContentTitle("Будильник")
                .setContentText("Время: $alarmTime")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle("Будильник")
                .setContentText("Время: $alarmTime")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        }

        notificationManager.notify(alarmTime.hashCode(), notification)
    }
}

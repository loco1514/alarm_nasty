package com.example.alarm_for_student

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.TimePicker
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
    BIWEEKLY,
    EVEN_WEEKS,
    ODD_WEEKS
}

enum class WeekDays {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

@Composable
fun AlarmScreen() {
    var alarms by remember { mutableStateOf(listOf<Alarm>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRepeatModeDialog by remember { mutableStateOf(false) }
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
            IconButton(onClick = { showTimePicker = true }) {
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
                    onEdit = { selectedAlarm = alarm; showRepeatModeDialog = true },
                    onDelete = {
                        cancelAlarm(context, alarm)
                        alarms = alarms.filter { it != alarm }
                    }
                )
            }
        }
    }

    if (showRepeatModeDialog && selectedAlarm != null) {
        ShowRepeatModeDialog(
            context = context,
            alarm = selectedAlarm!!,
            onRepeatModeSelected = { repeatMode, selectedDays ->
                selectedAlarm = selectedAlarm?.copy(repeatMode = repeatMode, repeatDays = selectedDays)
                selectedAlarm?.let {
                    alarms = alarms + it
                    scheduleAlarm(context, it)
                }
                showRepeatModeDialog = false
            },
            onDismiss = { showRepeatModeDialog = false }
        )
    }

    if (showTimePicker) {
        ShowTimePickerDialog(
            context = context,
            onTimeSelected = { hour, minute ->
                val formattedTime = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                selectedAlarm = Alarm(time = formattedTime)
                showTimePicker = false
                showRepeatModeDialog = true
            },
            onDismiss = { showTimePicker = false }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(text = alarm.time, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Повтор: ${when (alarm.repeatMode) {
                    RepeatMode.NONE -> "Нет"
                    RepeatMode.WEEKLY -> "Каждую неделю"
                    RepeatMode.BIWEEKLY -> "Раз в две недели"
                    RepeatMode.EVEN_WEEKS -> "Четные недели"
                    RepeatMode.ODD_WEEKS -> "Нечетные недели"
                }}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить будильник", tint = Color.Red)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowRepeatModeDialog(
    context: Context,
    alarm: Alarm,
    onRepeatModeSelected: (RepeatMode, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var repeatMode by remember { mutableStateOf(alarm.repeatMode) }
    var selectedDays by remember { mutableStateOf(alarm.repeatDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Выберите повторение") },
        text = {
            Column {
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
                            RepeatMode.BIWEEKLY -> "Раз в две недели"
                            RepeatMode.EVEN_WEEKS -> "Четные недели"
                            RepeatMode.ODD_WEEKS -> "Нечетные недели"
                        })
                    }
                }

                // Days of week selection
                if (repeatMode == RepeatMode.WEEKLY) {
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
            TextButton(onClick = { onRepeatModeSelected(repeatMode, selectedDays) }) {
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

@Composable
fun ShowTimePickerDialog(
    context: Context,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(hour, minute) },
            initialHour,
            initialMinute,
            true
        )
    }

    timePickerDialog.show()
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
            RepeatMode.BIWEEKLY -> {
                if (get(Calendar.WEEK_OF_YEAR) % 2 != 0) {
                    add(Calendar.WEEK_OF_YEAR, 1) // Start on next even week
                }
            }
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
        putExtra("repeatDays", alarm.repeatDays.toTypedArray())  // Передаем дни недели
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
                AlarmManager.INTERVAL_DAY * 7, // Weekly interval
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

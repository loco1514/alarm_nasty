package com.example.alarm_for_student

import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Alignment
import com.google.gson.Gson


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sharedPreferences: SharedPreferences) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showTeacherSchedule by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Расписание студентов") }
    var selectedGroup: Group? by remember { mutableStateOf(null) }
    var daySchedules by remember { mutableStateOf<List<DaySchedule>>(emptyList()) }
    var isGroupLoaded by remember { mutableStateOf(false) } // Флаг для отслеживания загрузки группы

    var savedGroupName by remember { mutableStateOf<String?>(null) } // Declare savedGroupName here

    // Загружаем выбранную группу из SharedPreferences
    LaunchedEffect(Unit) {
        savedGroupName = sharedPreferences.getString("selectedGroupName", null)
        // You might want to set isGroupLoaded = true here if this represents group loading completion.
    }

    // Проверка состояния перед рендером UI
    if (!isGroupLoaded) {
        // Вы можете показать индикатор загрузки или сообщение о том, что группа не загружена.
        Log.d("MainScreen", "Группа ещё не загружена")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onItemSelected = { selectedItem ->
                        coroutineScope.launch {
                            when (selectedItem) {
                                "Расписание студентов" -> {
                                    showTeacherSchedule = false
                                    title = "Расписание студентов"
                                }
                                "Расписание преподавателей" -> {
                                    showTeacherSchedule = true
                                    title = "Расписание преподавателей"
                                }
                                "Настройки" -> {
                                    title = "Настройки"
                                }
                            }
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title)
                            if (title == "Расписание студентов") {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        // Проверяем, что группа не null перед запросом расписания
                                        var groupedGroups = fetchGroups()
                                        selectedGroup = findGroupInShedule(savedGroupName.toString(), groupedGroups)
                                        val newSchedules = fetchSchedule(selectedGroup!!.link)
                                        Log.d("MainScreen", "Получено новое расписание.")
                                        saveScheduleToPreferences(sharedPreferences, selectedGroup!!.name, newSchedules, Gson())
                                        daySchedules = newSchedules
                                        Log.d("MainScreen", "Состояние обновлено.")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Обновить расписание"
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when {
                    showTeacherSchedule -> TeacherScheduleScreen(sharedPreferences)
                    title == "Настройки" -> SettingsScreen(sharedPreferences)
                    else -> GroupScheduleScreen(sharedPreferences, daySchedules)
                }
            }
        }
    }
}






@Composable
fun DrawerContent(onCloseDrawer: () -> Unit, onItemSelected: (String) -> Unit) {
    Column {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        DrawerItem("Расписание студентов", onItemSelected, onCloseDrawer)
        DrawerItem("Расписание преподавателей", onItemSelected, onCloseDrawer)
        DrawerItem("Настройки", onItemSelected, onCloseDrawer)
    }
}

@Composable
fun DrawerItem(title: String, onItemSelected: (String) -> Unit, onCloseDrawer: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onItemSelected(title)
                onCloseDrawer()
            }
            .padding(16.dp)
    )
}

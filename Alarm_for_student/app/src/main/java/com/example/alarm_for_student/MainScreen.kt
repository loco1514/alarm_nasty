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
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Alignment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(sharedPreferences: SharedPreferences) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showTeacherSchedule by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Расписание студентов") }
    var selectedGroup: Group? by remember { mutableStateOf(null) }
    var daySchedules by remember { mutableStateOf<List<DaySchedule>>(emptyList()) }
    var selectedWeek by remember { mutableStateOf(1) } // Store selected week here
    var weeksMap by remember { mutableStateOf(mapOf<Int, List<DaySchedule>>()) }
    var savedGroupName by remember { mutableStateOf<String?>(null) }
    var groupedGroups by remember { mutableStateOf(mapOf<String, List<Group>>()) } // Declare groupedGroups

    // Load schedule and selected group from SharedPreferences
    LaunchedEffect(Unit) {
        savedGroupName = sharedPreferences.getString("selectedGroupName", null)
        selectedWeek = sharedPreferences.getInt("selectedWeek", 1)

        // Load schedule data from SharedPreferences if available
        val savedWeeksJson = sharedPreferences.getString("weeksMap", null)
        if (savedWeeksJson != null) {
            weeksMap = Gson().fromJson(savedWeeksJson, object : TypeToken<Map<Int, List<DaySchedule>>>() {}.type)
            daySchedules = weeksMap[selectedWeek] ?: emptyList()
        }
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
                                        groupedGroups = fetchGroups()
                                        selectedGroup = findGroupInShedule(savedGroupName.toString(), groupedGroups)

                                        if (selectedGroup != null) {
                                            // Fetch and save the full weekly schedule
                                            weeksMap = fetchWeeks(selectedGroup!!.link)
                                            saveAllWeeksToPreferences(sharedPreferences, selectedGroup!!.name, weeksMap, Gson())

                                            // Update daySchedules for the selected week
                                            daySchedules = weeksMap[selectedWeek] ?: emptyList()

                                            Log.d("MainScreen", "Полное расписание обновлено.")
                                        } else {
                                            Log.d("MainScreen", "Группа не найдена.")
                                        }
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
                    else -> GroupScheduleScreen(
                        sharedPreferences = sharedPreferences,
                        initialDaySchedules = daySchedules // Match parameters to GroupScheduleScreen
                    )
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

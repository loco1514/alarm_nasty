package com.example.alarm_for_student

import android.content.SharedPreferences
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sharedPreferences: SharedPreferences) {
    // Example Drawer and state management code
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showTeacherSchedule by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Расписание студентов") }

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
                    title = { Text(title) },
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
                if (showTeacherSchedule) {
                    TeacherListScreen() // Ensure this is a valid Composable call
                } else {
                    GroupScheduleScreen(sharedPreferences)
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
        Divider()
        DrawerItem("Расписание студентов", onItemSelected, onCloseDrawer)
        DrawerItem("Расписание преподавателей", onItemSelected, onCloseDrawer)
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
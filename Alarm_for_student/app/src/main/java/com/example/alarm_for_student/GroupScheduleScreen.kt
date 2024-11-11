package com.example.alarm_for_student

import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.lang.reflect.Type

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GroupScheduleScreen(sharedPreferences: SharedPreferences, initialDaySchedules: List<DaySchedule>) {
    var groupedGroups by remember { mutableStateOf(mapOf<String, List<Group>>()) }
    var selectedFaculty by remember { mutableStateOf<String?>(null) }
    var filteredGroups by remember { mutableStateOf(listOf<Group>()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedDay by remember { mutableStateOf("Понедельник") }
    var selectedWeek by remember { mutableStateOf(1) } // Изначально устанавливаем неделю как 1
    var weeksMap by remember { mutableStateOf(mapOf<Int, String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var showTutor by remember { mutableStateOf(true) }
    var showRoom by remember { mutableStateOf(true) }
    val gson = Gson()
    val coroutineScope = rememberCoroutineScope()
    var daySchedules by remember { mutableStateOf(initialDaySchedules) }

    // Загружаем текущую неделю при инициализации компонента
    LaunchedEffect(Unit) {
        selectedWeek = getCurrentWeek() // Устанавливаем текущую неделю
        // Дальше логика загрузки групп, расписаний и т.д.
        showTutor = sharedPreferences.getBoolean("showTutor", true)
        showRoom = sharedPreferences.getBoolean("showRoom", true)

        val savedGroupName = sharedPreferences.getString("selectedGroupName", null) ?: ""
        groupedGroups = fetchGroups()
        selectedGroup = findGroupInShedule(savedGroupName, groupedGroups)

        val savedScheduleJson = sharedPreferences.getString("daySchedules", null)
        if (savedScheduleJson != null) {
            daySchedules = gson.fromJson(savedScheduleJson, object : TypeToken<List<DaySchedule>>() {}.type)
        } else if (selectedGroup != null) {
            daySchedules = fetchSchedule(selectedGroup!!.link)
            saveScheduleToPreferences(sharedPreferences, selectedGroup!!.name, daySchedules, gson)
        }
    }

    LaunchedEffect(selectedGroup) {
        if (selectedGroup != null) {
            weeksMap = fetchWeeks(selectedGroup!!.link)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            TextButton(onClick = { showDialog = true }) {
                Text(
                    text = selectedGroup?.name ?: "Выберите группу",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        WeekSelector(
            selectedWeek = selectedWeek,
            onWeekChange = { newWeek ->
                selectedWeek = newWeek
                coroutineScope.launch {
                    val weekLink = weeksMap[newWeek] ?: selectedGroup!!.link
                    daySchedules = fetchSchedule(weekLink)
                }
            },
            weeksMap = weeksMap
        )

        Spacer(modifier = Modifier.height(5.dp))

        DaySelector(
            selectedDay = selectedDay,
            onDayChange = { newDay -> selectedDay = newDay }
        )


        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Выберите факультет и группу") },
                text = {
                    LazyColumn {
                        groupedGroups.keys.forEach { faculty ->
                            item {
                                Text(
                                    text = faculty,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            selectedFaculty = faculty
                                            filteredGroups = groupedGroups[faculty] ?: emptyList()
                                        }
                                )
                            }
                            if (selectedFaculty == faculty) {
                                items(filteredGroups) { group ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            selectedGroup = group
                                            showDialog = false
                                            coroutineScope.launch {
                                                daySchedules = fetchSchedule(group.link)
                                                saveScheduleToPreferences(sharedPreferences, group.name, daySchedules, gson)
                                            }
                                        },
                                        headlineContent = { Text(text = group.name) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        if (daySchedules.isEmpty()) {
            Text(text = "Выберите группу для отображения расписания.")
        } else {
            val selectedDaySchedule = daySchedules.find { it.day.equals(selectedDay, ignoreCase = true) }
            ScheduleContent(
                daySchedule = selectedDaySchedule,
                showTutor = showTutor,
                showRoom = showRoom
            )
        }
    }
}

fun findGroupInShedule(
    findGroup: String,
    groupedGroups: Map<String, List<Group>>
): Group? {
    // Загружаем данные о группах
    Log.d("GroupScheduleScreen", "Список групп из fetchGroups: ${groupedGroups.keys}") // Лог для диагностики

    // Ищем группу по названию
    val allGroups = groupedGroups.values.flatten()
    val selectedGroup = allGroups.find { it.name == findGroup }

    Log.d("GroupScheduleScreen", "Найденная группа: ${selectedGroup?.name ?: "Не найдена"}") // Лог для диагностики
    return selectedGroup
}







@Composable
fun WeekSelector(selectedWeek: Int, onWeekChange: (Int) -> Unit, weeksMap: Map<Int, String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (selectedWeek > 1) { // Предотвращаем уход ниже первой недели
                    onWeekChange(selectedWeek - 1)
                }
            },
            enabled = selectedWeek > 1 // Отключаем кнопку, если это первая неделя
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Предыдущая неделя"
            )
        }

        Text(
            text = "Неделя: $selectedWeek",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(
            onClick = {
                if (selectedWeek < weeksMap.keys.maxOrNull() ?: selectedWeek) { // Ограничиваем до максимальной недели
                    onWeekChange(selectedWeek + 1)
                }
            },
            enabled = selectedWeek < (weeksMap.keys.maxOrNull() ?: selectedWeek) // Отключаем кнопку, если последняя неделя
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Следующая неделя"
            )
        }
    }
}


@Composable
fun DaySelector(selectedDay: String, onDayChange: (String) -> Unit) {
    val daysOfWeekFull = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"
    )

    val daysOfWeekAbbreviated = listOf(
        "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp), // уменьшенный padding вокруг Row
        horizontalArrangement = Arrangement.spacedBy(2.dp) // уменьшенный отступ между кнопками
    ) {
        daysOfWeekAbbreviated.forEachIndexed { index, abbreviatedDay ->
            val isSelected = daysOfWeekFull[index] == selectedDay
            Button(
                onClick = { onDayChange(daysOfWeekFull[index]) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(6.dp), // уменьшенный padding внутри кнопок
                modifier = Modifier
                    .width(60.dp) // уменьшенная ширина кнопок
                    .shadow(3.dp, RoundedCornerShape(4.dp))
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                Text(
                    text = abbreviatedDay,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize // уменьшенный шрифт текста
                )
            }
        }
    }
}


suspend fun fetchWeeks(scheduleUrl: String): Map<Int, String> {
    return withContext(Dispatchers.IO) {
        val weeksMap = mutableMapOf<Int, String>()
        try {
            val doc: Document = Jsoup.connect(scheduleUrl).get()
            val weekElements: Elements = doc.select("a[href^=?grp=][href*=week=]")
            for (element in weekElements) {
                val weekNumber = element.text().toIntOrNull()
                if (weekNumber != null) {
                    val weekLink = scheduleUrl + element.attr("href")
                    weeksMap[weekNumber] = weekLink
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении недель: ${e.message}", e)
        }
        weeksMap
    }
}





@Composable
fun ScheduleContent(daySchedule: DaySchedule?, showTutor: Boolean, showRoom: Boolean) {
    if (daySchedule == null || daySchedule.lessons.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Уроков нет.",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            val groupedLessons = daySchedule.lessons.groupBy { it.time }
            groupedLessons.forEach { (time, lessons) ->
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Время
                        Text(
                            text = time,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // Группировка по подгруппам
                        lessons.groupBy { it.subgroup ?: "Без подгруппы" }.forEach { (subgroup, subgroupLessons) ->
                            if (subgroup != "Без подгруппы") {
                                Text(
                                    text = "$subgroup п/г",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            subgroupLessons.forEach { lesson ->
                                LessonRow(lesson, showTutor, showRoom)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun LessonRow(lesson: Lesson, showTutor: Boolean, showRoom: Boolean) {
    // Обертка для урока
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .shadow(4.dp, shape = RoundedCornerShape(12.dp)) // Тень для выделения
            .padding(16.dp)
    ) {
        // Название дисциплины
        Text(
            text = "Дисциплина: ${lesson.subject}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Преподаватель (если включено)
        if (showTutor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Преподаватель",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Преподаватель: ${lesson.tutor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Аудитория (если включено)
        if (showRoom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Аудитория",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Аудитория: ${lesson.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Тип занятия
        Text(
            text = "Тип: ${lesson.type}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp)) // Отступ между уроками
}

suspend fun fetchGroups(): Map<String, List<Group>> {
    return withContext(Dispatchers.IO) {
        val groups = mutableListOf<Group>()
        try {
            val doc: Document = Jsoup.connect("https://rasp.ssuwt.ru/gs/faculties/").get()

            // Получаем все секции факультетов
            val facultyElements: Elements = doc.select("a[id^=mark-][role=tab]")
            val faculties = mutableMapOf<String, String>()

            // Сопоставляем ID факультетов с их названиями
            for (facultyElement in facultyElements) {
                val facultyId = facultyElement.attr("id")
                val facultyName = facultyElement.text()
                faculties[facultyId] = facultyName
            }

            // Получаем группы для каждого факультета
            for (facultyElement in facultyElements) {
                val facultyId = facultyElement.attr("aria-controls")
                val facultyName = facultyElement.text()

                // Выбираем группы под этим факультетом
                val groupElements: Elements = doc.select("#$facultyId a[href^=/gs/faculties/timeline?grp=]")

                for (groupElement in groupElements) {
                    val name = groupElement.text()
                    val link = "https://rasp.ssuwt.ru${groupElement.attr("href")}"
                    groups.add(Group(name, link, facultyName))
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при получении групп: ${e.message}", e)
        }

        // Группируем и сортируем группы по факультетам
        groups.groupBy { it.faculty }.mapValues { entry ->
            entry.value.sortedBy { it.name }  // Сортируем группы внутри каждого факультета по алфавиту
        }
    }
}

suspend fun fetchSchedule(link: String): List<DaySchedule> {
    return withContext(Dispatchers.IO) {
        val daySchedulesMap = mutableMapOf<String, MutableList<Lesson>>()
        try {
            val doc: Document = Jsoup.connect(link).get()
            val rows: Elements = doc.select("tr")
            for (row in rows) {
                val cells = row.select("td")
                if (cells.isEmpty()) continue
                val time = cells.getOrNull(0)?.text()?.trim() ?: "Нет данных"
                cells.forEachIndexed { index, cell ->
                    val lessonInfo = cell.text().trim()
                    if (lessonInfo.isEmpty()) return@forEachIndexed
                    val day = when (index) {
                        1 -> "Понедельник"
                        2 -> "Вторник"
                        3 -> "Среда"
                        4 -> "Четверг"
                        5 -> "Пятница"
                        6 -> "Суббота"
                        else -> return@forEachIndexed
                    }
                    val lessons = parseLessonInfo(lessonInfo)
                    lessons.forEach { lesson ->
                        val lessonWithTime = lesson.copy(time = time)
                        daySchedulesMap.getOrPut(day) { mutableListOf() }.add(lessonWithTime)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleDebug", "Ошибка при загрузке расписания: ${e.message}", e)
        }
        daySchedulesMap.map { DaySchedule(day = it.key, lessons = it.value) }
    }
}

private fun parseLessonInfo(lessonInfo: String): List<Lesson> {
    val regex = Regex("""(?<type>лаб|лек|пр|конс|ЭКЗ|зач)\s+(?<subject>.+?)\s+(?<room>\S+)\s+(?<tutor>[А-ЯЁ][а-яё]+\s+[А-ЯЁ]\.[А-ЯЁ]\.)(\s+(?<subgroup>\S+))?""")


    return regex.findAll(lessonInfo).map { match ->
        Lesson(
            time = "",
            type = match.groups["type"]?.value ?: "Неизвестно",
            subject = match.groups["subject"]?.value ?: "Неизвестно",
            room = match.groups["room"]?.value ?: "Неизвестно",
            tutor = match.groups["tutor"]?.value ?: "Неизвестно",
            subgroup = match.groups["subgroup"]?.value
        )
    }.toList()
}

suspend fun getCurrentWeek(): Int {
    return withContext(Dispatchers.IO) {
        val url = "https://rasp.ssuwt.ru/gs/faculties/"
        try {
            val document: Document = Jsoup.connect(url).get()

            // Находим элемент с классом "parity" или аналогичный элемент, который содержит информацию о текущей неделе
            val parityElement = document.select("div.parity").firstOrNull()

            // Извлекаем текст из найденного элемента
            val parityText = parityElement?.text() ?: throw IllegalStateException("Не удалось найти элемент с классом 'parity'")

            // Ищем номер недели в тексте, используя регулярное выражение
            val weekNumber = Regex("(\\d+) учебная неделя").find(parityText)?.groupValues?.get(1)
                ?: throw IllegalStateException("Не удалось найти номер недели в тексте")

            weekNumber.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Повторно выбрасываем исключение, если обработка не требуется
        }
    }
}


// Функция для сохранения расписания в SharedPreferences
fun saveScheduleToPreferences(
    sharedPreferences: SharedPreferences,
    groupName: String,
    daySchedules: List<DaySchedule>,
    gson: Gson
) {
    val daySchedulesJson = gson.toJson(daySchedules)
    sharedPreferences.edit().putString("selectedGroupName", groupName).apply()
    sharedPreferences.edit().putString("daySchedules", daySchedulesJson).apply()
}
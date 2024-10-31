package com.example.alarm_for_student

data class Lesson(
    val time: String,
    val type: String,
    val subject: String,
    val room: String,
    val tutor: String,
    val subgroup: String?
)

data class DaySchedule(
    val day: String,
    val lessons: List<Lesson>
)

data class Group(val name: String, val link: String)

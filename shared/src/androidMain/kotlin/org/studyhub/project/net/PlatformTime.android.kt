package org.studyhub.project.net

actual fun platformCurrentTimeHM(): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

actual fun platformTodayDayOfWeek(): Int {
    val cal = java.util.Calendar.getInstance()
    // Calendar: SUNDAY=1 … SATURDAY=7 → convert to 0=Domingo … 6=Sábado
    return (cal.get(java.util.Calendar.DAY_OF_WEEK) + 6) % 7
}

actual fun platformTodayIso(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

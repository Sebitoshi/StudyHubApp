package org.studyhub.project.net

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitWeekday

actual fun platformCurrentTimeHM(): String {
    val formatter = NSDateFormatter().apply { dateFormat = "HH:mm" }
    return formatter.stringFromDate(NSDate())
}

actual fun platformTodayDayOfWeek(): Int {
    val cal = NSCalendar.currentCalendar
    val weekday = cal.component(NSCalendarUnitWeekday, fromDate = NSDate())
    // NSCalendar: SUNDAY=1 … SATURDAY=7 → convert to 0=Domingo … 6=Sábado
    return (weekday.toInt() + 6) % 7
}

actual fun platformTodayIso(): String {
    val formatter = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd" }
    return formatter.stringFromDate(NSDate())
}

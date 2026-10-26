package org.studyhub.project.net

/** Hora local actual en formato HH:mm (24h), usada para marcar clases en curso/terminadas. */
expect fun platformCurrentTimeHM(): String

/** dayOfWeek en hora local (0=Domingo … 6=Sábado). */
expect fun platformTodayDayOfWeek(): Int

/** Fecha local de hoy en formato AAAA-MM-DD. */
expect fun platformTodayIso(): String

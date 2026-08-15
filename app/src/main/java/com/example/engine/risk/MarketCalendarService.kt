package com.example.engine.risk

import java.util.Calendar
import java.util.TimeZone

class MarketCalendarService {

    private val easternTimeZone = TimeZone.getTimeZone("America/New_York")

    /**
     * Checks if current time is within US Equities Regular Trading Hours (9:30 AM to 4:00 PM Eastern, Mon-Fri).
     */
    fun isMarketOpen(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val calendar = Calendar.getInstance(easternTimeZone).apply {
            timeInMillis = currentTimeMillis
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        // 9:30 AM (570 mins) to 4:00 PM (960 mins)
        return timeInMinutes in 570..960
    }

    /**
     * Returns true if near market close (e.g. within last 10 minutes of session).
     */
    fun isNearMarketClose(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val calendar = Calendar.getInstance(easternTimeZone).apply {
            timeInMillis = currentTimeMillis
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        return timeInMinutes in 950..960
    }
}

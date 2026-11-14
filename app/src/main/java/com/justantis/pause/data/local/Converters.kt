package com.justantis.pause.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Dates are stored as ISO text ("2026-11-14"), which sorts correctly and stays readable
 * in the database inspector.
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}

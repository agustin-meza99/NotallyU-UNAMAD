package com.example.notally.data

import androidx.room.TypeConverter
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.ThemeMode

/**
 * Conversores de Room para los enums del dominio. Se guardan como el nombre
 * del enum (`Priority.HIGH` -> `"HIGH"`), lo que mantiene las consultas SQL
 * legibles y estables ante reordenamientos.
 */
class Converters {

    @TypeConverter
    fun priorityToString(value: Priority): String = value.name

    @TypeConverter
    fun stringToPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun statusToString(value: Status): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): Status = Status.valueOf(value)

    @TypeConverter
    fun themeModeToString(value: ThemeMode): String = value.name

    @TypeConverter
    fun stringToThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)
}

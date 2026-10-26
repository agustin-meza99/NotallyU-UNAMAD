package com.example.notally.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.notally.data.local.CursoDao
import com.example.notally.data.local.SubtareaDao
import com.example.notally.data.local.TareaDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea

/**
 * Base de datos local. El nombre del archivo físico y la versión no cambian.
 */
@Database(
    entities = [Curso::class, Tarea::class, Subtarea::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NotallyDatabase : RoomDatabase() {

    abstract fun cursoDao(): CursoDao

    abstract fun tareaDao(): TareaDao

    abstract fun subtareaDao(): SubtareaDao

    companion object {
        const val NOMBRE = "notally.db"

        fun construir(context: Context): NotallyDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotallyDatabase::class.java,
                NOMBRE,
            ).build()
    }
}

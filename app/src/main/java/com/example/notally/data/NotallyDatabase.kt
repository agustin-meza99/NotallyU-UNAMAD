package com.example.notally.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.notally.data.local.SubjectDao
import com.example.notally.data.local.SubtaskDao
import com.example.notally.data.local.TaskDao
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Subtask
import com.example.notally.data.model.Task

@Database(
    entities = [Subject::class, Task::class, Subtask::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NotallyDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao

    abstract fun taskDao(): TaskDao

    abstract fun subtaskDao(): SubtaskDao

    companion object {
        const val NAME = "notally.db"

        fun build(context: Context): NotallyDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotallyDatabase::class.java,
                NAME,
            ).build()
    }
}

package com.example.notally.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notally.data.local.CursoDao
import com.example.notally.data.local.SubtareaDao
import com.example.notally.data.local.TareaDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea

/**
 * Base de datos local. El nombre del archivo físico se conserva; la versión 2
 * retira el color de los cursos.
 */
@Database(
    entities = [Curso::class, Tarea::class, Subtarea::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NotallyDatabase : RoomDatabase() {

    abstract fun cursoDao(): CursoDao

    abstract fun tareaDao(): TareaDao

    abstract fun subtareaDao(): SubtareaDao

    companion object {
        const val NOMBRE = "notally.db"

        /**
         * Migración 1 → 2: elimina la columna de color de `subjects` sin
         * perder filas.
         *
         * SQLite de API 30 no admite `ALTER TABLE ... DROP COLUMN`, así que se
         * recrea la tabla con las columnas supervivientes, se copian los datos,
         * se sustituye la original y se restaura el índice único de `name`.
         * Las claves foráneas de `tasks.subjectId` siguen apuntando a
         * `subjects(id)`.
         */
        val MIGRACION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subjects_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`professor` TEXT, " +
                        "`credits` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `subjects_new` " +
                        "(`id`, `name`, `professor`, `credits`, `createdAt`) " +
                        "SELECT `id`, `name`, `professor`, `credits`, `createdAt` " +
                        "FROM `subjects`",
                )
                db.execSQL("DROP TABLE `subjects`")
                db.execSQL("ALTER TABLE `subjects_new` RENAME TO `subjects`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_subjects_name` " +
                        "ON `subjects` (`name`)",
                )
            }
        }

        fun construir(context: Context): NotallyDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotallyDatabase::class.java,
                NOMBRE,
            )
                .addMigrations(MIGRACION_1_2)
                .build()
    }
}

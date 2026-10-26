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
 * retira el color de los cursos, la 3 añade el `externalId` del Aula Virtual y
 * la 4 añade datos opcionales del curso y de la tarea.
 */
@Database(
    entities = [Curso::class, Tarea::class, Subtarea::class],
    version = 4,
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

        /**
         * Migración 2 → 3: añade `externalId` (id del Aula Virtual) a cursos y
         * tareas, con índice único. Es aditiva: las filas existentes conservan
         * todos sus datos y quedan con `externalId` nulo.
         */
        val MIGRACION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `externalId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_subjects_externalId` " +
                        "ON `subjects` (`externalId`)",
                )
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `externalId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_externalId` " +
                        "ON `tasks` (`externalId`)",
                )
            }
        }

        /**
         * Migración 3 → 4: añade los datos opcionales del Aula Virtual. Es
         * aditiva: las filas existentes conservan todos sus datos y las nuevas
         * columnas quedan nulas (o `0` para `esGrupal`).
         */
        val MIGRACION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `codigo` TEXT")
                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `seccion` TEXT")
                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `periodo` TEXT")
                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `tipo` TEXT")

                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `unidad` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `inicioEn` INTEGER")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `intentos` INTEGER")
                db.execSQL(
                    "ALTER TABLE `tasks` ADD COLUMN `esGrupal` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun construir(context: Context): NotallyDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotallyDatabase::class.java,
                NOMBRE,
            )
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
                .build()
    }
}

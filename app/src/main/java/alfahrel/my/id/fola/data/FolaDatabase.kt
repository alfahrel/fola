package alfahrel.my.id.fola.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import alfahrel.my.id.fola.data.dao.TaskDao
import alfahrel.my.id.fola.data.model.Task

@Database(
    entities = [Task::class],
    version = 2,
    exportSchema = false
)
abstract class FolaDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: FolaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN startDateMs INTEGER")
            }
        }

        fun getInstance(context: Context): FolaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FolaDatabase::class.java,
                    "fola_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
package com.example.lockinplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lockinplanner.data.local.dao.TaskDao
import com.example.lockinplanner.data.local.dao.ChecklistDao
import com.example.lockinplanner.data.local.entity.TaskEntity
import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity
import com.example.lockinplanner.data.local.entity.BookEntity
import com.example.lockinplanner.data.local.entity.ChapterEntity
import com.example.lockinplanner.data.local.entity.PageEntity
import com.example.lockinplanner.data.local.entity.ShortEntity
import com.example.lockinplanner.data.local.dao.BookDao
import com.example.lockinplanner.data.local.dao.ChapterDao
import com.example.lockinplanner.data.local.dao.PageDao
import com.example.lockinplanner.data.local.dao.ShortDao

import androidx.room.AutoMigration

@Database(
    entities = [
        TaskEntity::class, 
        ChecklistEntity::class, 
        ObjectiveEntity::class,
        BookEntity::class,
        ChapterEntity::class,
        PageEntity::class,
        ShortEntity::class
    ], 
    version = 8,  
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun pageDao(): PageDao
    abstract fun shortDao(): ShortDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN isThemeColor INTEGER NOT NULL DEFAULT 0")
                    }
                }

                val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `chapters` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_bookId` ON `chapters` (`bookId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `pages` (`id` TEXT NOT NULL, `chapterId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pages_chapterId` ON `pages` (`chapterId`)")
                    }
                }

                val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN tag TEXT DEFAULT NULL")
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lockin_planner_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

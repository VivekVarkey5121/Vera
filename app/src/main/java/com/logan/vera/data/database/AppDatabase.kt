package com.logan.vera.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.logan.vera.data.database.dao.BookDao
import com.logan.vera.data.database.entities.Book
import com.logan.vera.data.database.entities.Tag
import com.logan.vera.data.database.entities.BookTagCrossRef
import com.logan.vera.data.database.entities.BookCollection
import com.logan.vera.data.database.entities.BookCollectionEntry

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Book::class,
        BookCollection::class,
        BookCollectionEntry::class,
        Tag::class,
        BookTagCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vera_database"
                ).addMigrations(MIGRATION_1_2) // Use this
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new 'tags' table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
        
        // Create the unique index on 'name'
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

        // Create the join table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `book_tag_cross_ref` (
                `bookId` TEXT NOT NULL, 
                `tagId` TEXT NOT NULL, 
                PRIMARY KEY(`bookId`, `tagId`), 
                FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """)
        
        // Create the index on 'tagId'
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_tag_cross_ref_tagId` ON `book_tag_cross_ref` (`tagId`)")
    }
}
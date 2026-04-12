package com.logan.vera.data.database.dao

import androidx.room.*
import com.logan.vera.data.database.entities.Book
import com.logan.vera.data.database.entities.Tag
import com.logan.vera.data.database.entities.BookTagCrossRef

import com.logan.vera.data.database.entities.BookCollection
import com.logan.vera.data.database.entities.BookCollectionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    // Basic book operations
    @Query("""
        SELECT * FROM books 
        WHERE id NOT IN (
            SELECT bt.bookId FROM book_tag_cross_ref bt
            INNER JOIN tags t ON bt.tagId = t.id
            WHERE t.name = '#hidden'
        )
        ORDER BY lastAccessed DESC
    """)
    fun getAllBooksFilter(): Flow<List<Book>>




    @Query("""
        SELECT * FROM books 
        ORDER BY lastAccessed DESC
    """)
    fun getAllBooks(): Flow<List<Book>>


    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBook(bookId: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    // Enhanced reading progress tracking
    @Query("""
        UPDATE books SET 
            lastReadChapterIndex = :chapterIndex, 
            lastReadPosition = :position, 
            lastAccessed = :timestamp,
            totalChapters = :totalChapters,
            readProgress = :progress,
            timeSpentReading = timeSpentReading + :timeSpent,
            lastReadDate = :timestamp
        WHERE id = :bookId
    """)
    suspend fun updateReadingProgress(
        bookId: String,
        chapterIndex: Int,
        position: Float,
        totalChapters: Int,
        progress: Float,
        timeSpent: Long = 0,
        timestamp: Long = System.currentTimeMillis()
    )

    // Search functionality
    @Query("""
        SELECT * FROM books 
        WHERE (
            -- Standard search: Match title/author AND must not be hidden
            ((title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
            AND id NOT IN (
                SELECT bt.bookId FROM book_tag_cross_ref bt
                INNER JOIN tags t ON bt.tagId = t.id
                WHERE LOWER(t.name) = '#hidden'
            ))
        )
    """)
    fun searchBooks(query: String): Flow<List<Book>>

    // Collection operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: BookCollection)

    @Delete
    suspend fun deleteCollection(collection: BookCollection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionEntry(entry: BookCollectionEntry)

    @Delete
    suspend fun deleteCollectionEntry(entry: BookCollectionEntry)

    @Query("SELECT * FROM book_collections ORDER BY sortOrder ASC")
    fun getAllCollections(): Flow<List<BookCollection>>

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_collection_entries bce ON b.id = bce.bookId
        WHERE bce.collectionId = :collectionId
        ORDER BY bce.sortOrder ASC, b.lastAccessed DESC
    """)
    fun getBooksInCollection(collectionId: String): Flow<List<Book>>

    @Transaction
    @Query("SELECT * FROM book_collections WHERE id = :collectionId")
    suspend fun getCollection(collectionId: String): BookCollection?

    @Query("DELETE FROM book_collection_entries WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBookFromCollection(bookId: String, collectionId: String)

    @Query("UPDATE book_collections SET coverBookId = :bookId WHERE id = :collectionId")
    suspend fun updateCollectionCover(collectionId: String, bookId: String?)

    @Query("UPDATE books SET coverPath = NULL WHERE id = :bookId")
    suspend fun clearCoverPath(bookId: String)

    //tags
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("""
        SELECT * FROM tags 
        WHERE name LIKE '%' || :query || '%' 
        AND (
            LOWER(name) != '#hidden' 
            OR LOWER(:query) = '#hidden'
        )
        ORDER BY name ASC
    """)
    fun searchTags(query: String): Flow<List<Tag>>

    // Linking Books and Tags
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookTagCrossRef(crossRef: BookTagCrossRef)

    @Query("DELETE FROM book_tag_cross_ref WHERE bookId = :bookId AND tagId = :tagId")
    suspend fun deleteBookTagCrossRef(bookId: String, tagId: String)

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_tag_cross_ref bt ON b.id = bt.bookId
        INNER JOIN tags t ON bt.tagId = t.id
        WHERE t.name LIKE :tagName || '%'
        AND (
            -- Condition A: The tag is NOT #hidden (so we must check if the book is hidden)
            LOWER(t.name) != '#hidden' 
            AND b.id NOT IN (
                SELECT bt2.bookId FROM book_tag_cross_ref bt2
                INNER JOIN tags t2 ON bt2.tagId = t2.id
                WHERE LOWER(t2.name) = '#hidden'
            )
            OR
            -- Condition B: The tag IS #hidden (so we must show the book)
            LOWER(t.name) = '#hidden'
        )
    """)
    fun getBooksByTagName(tagName: String): Flow<List<Book>>

    //get tags per book
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN book_tag_cross_ref bt ON t.id = bt.tagId
        WHERE bt.bookId = :bookId
    """)
    fun getTagsForBook(bookId: String): Flow<List<Tag>>



}

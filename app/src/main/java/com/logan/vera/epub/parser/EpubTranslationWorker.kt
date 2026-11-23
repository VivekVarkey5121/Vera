package com.logan.vera.epub.parser

import android.content.Context
import androidx.work.WorkerParameters
import com.logan.vera.data.models.ChapterModel
import kotlinx.coroutines.flow.first
import java.io.File
import com.logan.vera.data.repository.BookRepository
import com.logan.vera.data.repository.TranslationRepository
import com.logan.vera.epub.parser.EpubParser

import dagger.assisted.AssistedInject
import dagger.assisted.Assisted

import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.workDataOf
import com.logan.vera.data.database.entities.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class EpubTranslationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: BookRepository,
    private val epubTranslator: EpubTranslator
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val bookId = inputData.getString(KEY_BOOK_ID)
            ?: return@withContext Result.failure()
        try {
            val book = repository.returnBookByID(bookId) as? Book
                ?: throw IllegalStateException("Invalid or missing Book for id=$bookId")
            epubTranslator.translateEpub( //The function call and how it is handled (full book vs chapters? TODO - book, translateEpub handles chapters
                sourceBook = book,
                onProgress = { currentChapter: Int, totalChapters: Int ->
                    setProgress(
                        workDataOf(
                            KEY_BOOK_ID to bookId,
                            KEY_PROGRESS to (currentChapter.toFloat() / totalChapters)
                        )
                    )
                }
            )

        Result.success()
    } catch (e: Exception) {
        // Use Result.retry() for transient issues (e.g., waiting for Wi-Fi model download),
        // or Result.failure() for permanent errors
        Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Translation failed")))
    }
}

companion object {
    const val KEY_BOOK_ID = "book_id"
    const val KEY_PROGRESS = "progress"
    const val KEY_ERROR = "error"
    const val TAG_TRANSLATION = "book_translation"
}
}
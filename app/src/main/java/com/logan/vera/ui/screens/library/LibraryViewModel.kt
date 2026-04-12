package com.logan.vera.ui.screens.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logan.vera.data.models.BookModel
import com.logan.vera.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map


private const val TAG = "LibraryViewModel"



@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val books = repository.allBooksFilter
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
             initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredBooks = _searchQuery.flatMapLatest { query ->
        if (query.startsWith("#") && query.length > 1) {
            val tagName = query.substring(1)
            repository.getBooksWithTag(tagName)
        } else {
            repository.allBooksFilter.map { books ->
                if (query.isBlank()) {
                    books
                } else {
                    books.filter { book ->
                        book.title.contains(query, ignoreCase = true) ||
                        book.author?.contains(query, ignoreCase = true) == true
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    val selectedBooks = mutableStateListOf<String>()

    fun toggleSelection(bookId: String) {
        if (selectedBooks.contains(bookId)) {
            selectedBooks.remove(bookId)
        } else {
            selectedBooks.add(bookId)
        }
    }
    fun clearSelection() {
        selectedBooks.clear()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            try {
                val idsToDelete = selectedBooks.toList()
                
                repository.deleteBooks(idsToDelete)
                
                clearSelection()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete selected books", e)
            }
        }
    }

    fun deleteBookCover(){
        viewModelScope.launch {
            try {
                val idsToDelete = selectedBooks.toList()
                
                for (id in idsToDelete){
                    repository.deleteBookCover(id)
                }

                clearSelection()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete book cover", e)
            }
        }
    }

    fun onSearchQueryChanged(searchQuery : String){
        _searchQuery.value = searchQuery
    }

    fun addBookTag(tagName: String){
        viewModelScope.launch {
            try {
                val idsToTag = selectedBooks.toList()
                
                for (id in idsToTag){
                    repository.addTagToBook(id, tagName)
                }

                clearSelection()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add tag", e)
            }
        }
    }


    fun removeBookTag(tagName: String){
        viewModelScope.launch {
            try {
                val idsToTag = selectedBooks.toList()
                
                for (id in idsToTag){
                    repository.removeTagToBook(id, tagName)
                }

                clearSelection()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove tag", e)
            }
        }
    }


    suspend fun addBook(fileBytes: ByteArray, fileName: String) {
        try {
            Log.d(TAG, "Adding book: $fileName")
            repository.addBook(fileBytes, fileName)
            Log.d(TAG, "Successfully added book: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding book: $fileName", e)
            throw e
        }
    }
}

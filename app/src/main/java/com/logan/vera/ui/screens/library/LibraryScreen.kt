package com.logan.vera.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.logan.vera.ui.components.BookCard
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* 
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val selectedBooks = viewModel.selectedBooks
    val inSelectMode = selectedBooks.isNotEmpty()
    val books by viewModel.filteredBooks.collectAsState(initial = emptyList())
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    var showTagDialog by remember { mutableStateOf(false) }
    var tagName by remember { mutableStateOf("") }

    // Sort books with last accessed first
    val sortedBooks = remember(books) {
        val lastBook = books.maxByOrNull { it.lastAccessed }
        if (lastBook != null) {
            listOf(lastBook) + books.filter { it.id != lastBook.id }
        } else {
            books
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val fileName = it.lastPathSegment ?: "unknown.epub"
                        inputStream?.use { stream ->
                            viewModel.addBook(
                                fileBytes = stream.readBytes(),
                                fileName = fileName
                            )
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = e.message ?: "Failed to add book",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { 
            if (inSelectMode){
                TopAppBar(
                    title = { Text("${selectedBooks .size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        } 

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add Tag") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        showTagDialog = true
                                        Log.d("Library", "Dialog state: $showTagDialog")
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove Cover") },
                                    //leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.deleteBookCover()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }else{
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (searchActive) 0.dp else 16.dp),
                    query = searchQuery,
                    onQueryChange = { searchQuery = it; viewModel.onSearchQueryChanged(it) },
                    onSearch = { searchActive = false },
                    active = false,
                    onActiveChange = { searchActive = it; if (it == false){searchQuery = ""; viewModel.onSearchQueryChanged("") }},
                    placeholder = { Text("Search your library...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchActive) {
                            IconButton(onClick = { 
                                if (searchQuery.isNotEmpty()) searchQuery = "" else searchActive = false;viewModel.onSearchQueryChanged("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    }
                ) {
                    // This is the "suggestions" area that shows when the search bar is active
                    // You can leave it empty or show recent searches
                }
            }
        }

    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your library is empty",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            items(sortedBooks, key = { it.id }) { book ->
                val isSelected = selectedBooks .contains(book.id)
                
                BookCard(
                    book = book,
                    isSelected = isSelected, // Pass the state down
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (inSelectMode) {
                                viewModel.toggleSelection(book.id)
                            } else {
                                onBookClick(book.id)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelection(book.id)
                            }
                        )
                    )
                }
            }
        }
        if (showTagDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showTagDialog = false
                    tagName = "" 
                },
                title = { Text("Add Tag") },
                text = {
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text("Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // REMOVE BUTTON
                        TextButton(
                            onClick = {
                                if (tagName.isNotBlank()) {
                                    viewModel.removeBookTag(tagName) 
                                    showTagDialog = false
                                    tagName = ""
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error // Red color for removal
                            )
                        ) {
                            Text("Remove")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // ADD BUTTON
                        Button(
                            onClick = {
                                if (tagName.isNotBlank()) {
                                    viewModel.addBookTag(tagName)
                                    showTagDialog = false
                                    tagName = ""
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showTagDialog = false
                        tagName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }



}

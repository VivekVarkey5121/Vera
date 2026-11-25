package com.logan.vera.ui.reader

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.logan.vera.data.models.ChapterModel
import com.logan.vera.epub.utils.BookTextMapper
import com.logan.vera.epub.utils.toAnnotatedString
import com.logan.vera.ui.theme.ReaderTheme
import com.logan.vera.utils.TimerLock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    onNavigateUp: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val translationState by viewModel.selectionTranslation.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var isInitialized by remember { mutableStateOf(false) }
    var showBars by remember { mutableStateOf(true) }
    var horizontalDragOffset by remember { mutableStateOf(0f) } // for the chapter drag
    val coverHeight = 300.dp

    // System UI controller
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Initial scroll to saved position
    LaunchedEffect(chapters, uiState.currentChapterIndex, uiState.scrollPosition) {
        if (chapters.isNotEmpty() && !isInitialized) {
            listState.scrollToItem(
                index = uiState.currentChapterIndex,
                scrollOffset = with(density) { uiState.scrollPosition.dp.toPx().toInt() }
            )
            isInitialized = true
        }
    }

    // Save reading progress when scrolling (debounced)
    LaunchedEffect(listState) {
        snapshotFlow {
            ReadingPosition(
                chapterIndex = listState.firstVisibleItemIndex,
                scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp().value }
            )
        }
            .distinctUntilChanged()
            .collect { position ->
                if (isInitialized && position.chapterIndex >= 0) {
                    viewModel.saveReadingProgress(position)
                }
            }
    }

    // Back handler
    BackHandler {
        coroutineScope.launch {
            val position = ReadingPosition(
                chapterIndex = listState.firstVisibleItemIndex,
                scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp().value }
            )
            viewModel.saveReadingProgress(position)
            onNavigateUp()
        }
    }

    var selectionDismissKey by remember { mutableStateOf(0) }
    var interceptedSelectionText by remember { mutableStateOf<String?>(null) }
    var interceptNextCopy by remember { mutableStateOf(false) }
    val androidClipboard = remember(view) {
        view.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager
    }

    val inMemoryClipboard = remember {
        object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText(): androidx.compose.ui.text.AnnotatedString? {
                return interceptedSelectionText?.let { androidx.compose.ui.text.AnnotatedString(it) }
            }
            override fun setText(annotatedString: androidx.compose.ui.text.AnnotatedString) {
                interceptedSelectionText = annotatedString.text
                if (!interceptNextCopy) {
                    androidClipboard?.setPrimaryClip(
                        android.content.ClipData.newPlainText("selected text", annotatedString.text)
                    )
                }
            }
            override fun hasText(): Boolean = !interceptedSelectionText.isNullOrEmpty()
        }
    }

    val customToolbar = remember(view) {
        TranslateTextToolbar(
            view = view,
            onTranslateClicked = { copyAction ->
                interceptNextCopy = true
                copyAction?.invoke()
                interceptNextCopy = false
                interceptedSelectionText
                    ?.takeIf { it.isNotBlank() }
                    ?.let(viewModel::translateSelection)
                interceptedSelectionText = null
                selectionDismissKey++
            }
        )
    }

    // Dismiss selection when scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            customToolbar.dismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(chapters.size, translationState != null) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = 200 // How many pixels they must swipe to trigger a change

                        if (translationState == null && chapters.isNotEmpty()) {
                            if (horizontalDragOffset > threshold) {
                                // Swiped Right (Go to Previous Chapter)
                                val prevChapter = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(prevChapter)
                                }
                            } else if (horizontalDragOffset < -threshold) {
                                // Swiped Left (Go to Next Chapter)
                                val nextChapter = (listState.firstVisibleItemIndex + 1).coerceAtMost(chapters.size - 1)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(nextChapter)
                                }
                            }
                        }
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (translationState == null) {
                            change.consume()
                            horizontalDragOffset += dragAmount
                        } else {
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = showBars,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ReaderTopBar(
                        title = uiState.title,
                        onNavigateUp = {
                            coroutineScope.launch {
                                val position = ReadingPosition(
                                    chapterIndex = listState.firstVisibleItemIndex,
                                    scrollOffset = with(density) {
                                        listState.firstVisibleItemScrollOffset.toDp().value
                                    }
                                )
                                viewModel.saveReadingProgress(position)
                                onNavigateUp()
                            }
                        },
                        onTimerClick = {
                            TimerLock.setLockDuration(context, TimerLock.getForceLockMins(context))
                            TimerLock.resetAccumulatedTime(context)
                        },
                        onTimeLeftClick = {
                            val message = "You have been reading for ${TimerLock.formatMillis(TimerLock.getAccumulatedTime(context))}"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        onReturnToTopClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showBars = !showBars
                        val window = (view.context as Activity).window
                        val controller = WindowCompat.getInsetsController(window, view)
                        if (showBars) {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        } else {
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.isLocked) { // timer lock
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Focus Lock Active",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Take a break.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            val message = "You have ${TimerLock.formatMillis(TimerLock.getRemainingSeconds(context))} left"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Check Timer")
                        }
                    }
                } else {
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    } ?: CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalTextToolbar provides customToolbar,
                        androidx.compose.ui.platform.LocalClipboardManager provides inMemoryClipboard
                    ) {
                        key(selectionDismissKey) {
                            androidx.compose.foundation.text.selection.SelectionContainer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                customToolbar.dismiss()
                                                selectionDismissKey++
                                                showBars = !showBars
                                            }
                                        )
                                    }
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(uiState.theme.background),
                                    userScrollEnabled = true
                                ) {
                                    // Cover image
                                    item(key = "cover") {
                                        uiState.coverImage?.let { coverImageData ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp)
                                            ) {
                                                AsyncImage(
                                                    model = coverImageData,
                                                    contentDescription = "Book cover",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(coverHeight),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                            HorizontalDivider()
                                        }
                                    }

                                    // Chapters
                                    items(
                                        items = chapters,
                                        key = { it.index }
                                    ) { chapter ->
                                        ChapterContent(
                                            chapter = chapter,
                                            fontSize = uiState.fontSize,
                                            fontFamily = uiState.fontFamily,
                                            theme = uiState.theme,
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        translationState?.let { state ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissSelectionTranslation() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = state.sourceText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    when {
                        state.isLoading -> CircularProgressIndicator()
                        state.error != null -> Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        state.translatedText != null -> Text(
                            text = state.translatedText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ChapterContent(
    chapter: ChapterModel,
    fontSize: Float,
    fontFamily: FontFamily,
    theme: ReaderTheme,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.background)
    ) {
        if (chapter.title.isNotEmpty()) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = fontFamily,
                color = theme.text,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        val parts = remember(chapter.content) {
            chapter.content.split("<img")
        }

        parts.forEachIndexed { index, part ->
            if (index == 0) {
                Text(
                    text = part.toAnnotatedString(),
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5).sp,
                    fontFamily = fontFamily,
                    color = theme.text
                )
            } else {
                val imgEndIndex = part.indexOf("/>")
                if (imgEndIndex != -1) {
                    val imgTag = part.substring(0, imgEndIndex + 2)
                    val text = part.substring(imgEndIndex + 2)

                    BookTextMapper.parseImgTag(imgTag)?.let { img ->
                        val imageData = viewModel.getImageData(img.path)
                        if (imageData != null) {
                            AsyncImage(
                                model = imageData,
                                contentDescription = "Book image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f / img.yrel)
                                    .padding(vertical = 8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    if (text.isNotEmpty()) {
                        Text(
                            text = text.toAnnotatedString(),
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.5).sp,
                            fontFamily = fontFamily,
                            color = theme.text
                        )
                    }
                }
            }
        }
    }
}
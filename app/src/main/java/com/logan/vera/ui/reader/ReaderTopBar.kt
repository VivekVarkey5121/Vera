package com.logan.vera.ui.reader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    onNavigateUp: () -> Unit,
    onTimeLeftClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },

        actions = {
            IconButton(onClick = onTimerClick) {
                Icon(
                    imageVector = Icons.Default.Lock, 
                    contentDescription = "Lock App"
                )
            }
            IconButton(onClick = onTimeLeftClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "time remaining"
                )
            }
        },
        modifier = modifier
    )
}

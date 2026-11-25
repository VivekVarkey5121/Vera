package com.logan.vera.ui.reader

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun SelectableChapterText(
    annotatedText: AnnotatedString,
    fontSize: Float,
    fontFamily: FontFamily,
    textColor: Color,
    onTranslateRequested: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val view = LocalView.current

    val customToolbar = remember(view) {
        TranslateTextToolbar(
            view = view,
            onTranslateClicked = { copyAction ->
                val prevClip = clipboardManager.getText()
                copyAction?.invoke() // Copy selected text to clipboard
                val selectedText = clipboardManager.getText()?.text.orEmpty()
                if (prevClip != null) {
                    clipboardManager.setText(prevClip) // Restore original clipboard content
                }
                if (selectedText.isNotBlank()) {
                    onTranslateRequested(selectedText)
                }
            }
        )
    }

    CompositionLocalProvider(LocalTextToolbar provides customToolbar) {
        SelectionContainer(modifier = modifier) {
            Text(
                text = annotatedText,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.5).sp,
                fontFamily = fontFamily,
                color = textColor
            )
        }
    }
}

/**
 * Custom TextToolbar that delegates to Android's Floating ActionMode 
 * and injects a "Translate" item into the floating selection menu.
 */
class TranslateTextToolbar(
    private val view: View,
    private val onTranslateClicked: ((() -> Unit)?) -> Unit,
    private val onOriginalClicked: ((() -> Unit)?) -> Unit
) : TextToolbar {
    private var actionMode: ActionMode? = null
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    fun dismiss() {
        actionMode?.finish()
        actionMode = null
        status = TextToolbarStatus.Hidden
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        status = TextToolbarStatus.Shown

        val callback = object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (menu == null) return false
                if (onCopyRequested != null) {
                    menu.add(Menu.NONE, android.R.id.copy, 0, android.R.string.copy)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
                if (onSelectAllRequested != null) {
                    menu.add(Menu.NONE, android.R.id.selectAll, 1, android.R.string.selectAll)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
                // Custom Translate action item in the floating toolbar
                menu.add(Menu.NONE, 9999, 2, "Translate")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                // Custom Translate action item in the floating toolbar
                menu.add(Menu.NONE, 9998, 2, "Original")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    android.R.id.copy -> {
                        onCopyRequested?.invoke()
                        mode?.finish()
                        return true
                    }
                    android.R.id.selectAll -> {
                        onSelectAllRequested?.invoke()
                        return true
                    }
                    9999 -> {
                        onTranslateClicked(onCopyRequested)
                        mode?.finish()
                        return true
                    }
                    9998 -> {
                        onOriginalClicked(onCopyRequested)
                        mode?.finish()
                        return true
                    }
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                actionMode = null
                status = TextToolbarStatus.Hidden
            }

            override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: android.graphics.Rect?) {
                outRect?.set(
                    rect.left.toInt(),
                    rect.top.toInt(),
                    rect.right.toInt(),
                    rect.bottom.toInt()
                )
            }
        }

        actionMode?.finish()
        actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }
}


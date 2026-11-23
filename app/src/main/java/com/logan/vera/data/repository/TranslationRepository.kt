package com.logan.vera.data.repository

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationRepository(
    sourceLanguage: String = TranslateLanguage.ENGLISH,
    targetLanguage: String = TranslateLanguage.SPANISH
) {
    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
    )

    suspend fun ensureModelDownloaded(wifiOnly: Boolean = true) {
        val conditions = DownloadConditions.Builder().apply {
            if (wifiOnly) requireWifi()
        }.build()

        suspendCancellableCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    suspend fun translate(text: String): String {
        if (text.isBlank()) return text
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { translated ->
                    if (cont.isActive) cont.resume(translated)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    fun close() {
        translator.close()
    }
}
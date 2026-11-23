package com.logan.vera.epub.parser

import android.content.Context
import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.logan.vera.data.database.entities.Book
import com.logan.vera.data.models.ChapterModel
import com.logan.vera.data.repository.BookRepository
import com.logan.vera.data.repository.TranslationRepository
import com.logan.vera.epub.parser.EpubParser.Companion.ManifestItem
import com.logan.vera.epub.parser.EpubParser.Companion.isChapter
import com.logan.vera.epub.utils.EpubFile
import com.logan.vera.epub.utils.decodedURL
import com.logan.vera.epub.utils.getAttributeValue
import com.logan.vera.epub.utils.parseXMLFile
import com.logan.vera.epub.utils.selectChildTag
import com.logan.vera.epub.utils.selectFirstTag
import com.logan.vera.ui.reader.ReaderUiState
import com.logan.vera.ui.reader.ReadingPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.io.path.invariantSeparatorsPathString

class EpubTranslator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BookRepository
) {
    private val tag = "EpubTranslator"
    private var epubImages: Map<String, ByteArray> = emptyMap()
    private var currentBookId: String = ""
    private var lastSavedPosition: ReadingPosition? = null

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterModel>>(emptyList())
    val chapters = _chapters.asStateFlow()

    suspend fun translateEpub(
        sourceBook: Book,
        onProgress: suspend (currentChapter: Int, totalChapters: Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        try {
            val files = EpubParser.getZipFiles(File(sourceBook.filePath).inputStream())

            val container = files["META-INF/container.xml"]
                ?: throw Exception("META-INF/container.xml file missing")

            val opfFilePath = parseXMLFile(container.data)
                ?.selectFirstTag("rootfile")
                ?.getAttributeValue("full-path")
                ?.decodedURL ?: throw Exception("Invalid container.xml file")

            val rootPath = opfFilePath.substringBefore('/', "")
            val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

            val document = parseXMLFile(opfFile.data)
                ?: throw Exception(".opf file failed to parse data")

            val manifest = document.selectFirstTag("manifest")
                ?: throw Exception(".opf file manifest section missing")
            val spine = document.selectFirstTag("spine")
                ?: throw Exception(".opf file spine section missing")

            val hrefRootPath = File(opfFilePath).parentFile ?: File("")
            fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
                .toPath()
                .invariantSeparatorsPathString
                .removePrefix("/")

            val manifestItems = manifest.selectChildTag("item").map {
                ManifestItem(
                    id = it.getAttribute("id"),
                    absPath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                    mediaType = it.getAttribute("media-type"),
                    properties = it.getAttribute("properties")
                )
            }.associateBy { it.id }

            val chapters = mutableListOf<Pair<String, String>>()
            val translatedFiles = files.toMutableMap()

            spine.selectChildTag("itemref").forEach { itemRef ->
                val itemId = itemRef.getAttribute("idref")
                val spineItem = manifestItems[itemId]

                if (spineItem != null && isChapter(spineItem)) {
                    var spineUrl = spineItem.absPath
                    if (rootPath.isNotEmpty() && !spineUrl.startsWith("$rootPath/") && !files.containsKey(spineUrl)) {
                        spineUrl = "$rootPath/$spineUrl"
                    }

                    val body = files[spineUrl]?.data?.toString(Charsets.UTF_8)
                    if (body != null) {
                        chapters.add(spineUrl to body)
                    }
                }
            }

            val sourceFile = File(sourceBook.filePath)
            val outputDir = sourceFile.parentFile ?: File(context.filesDir, "books")
            val targetEpubFile = File(outputDir, "${sourceFile.nameWithoutExtension}_es.epub")

            val jobTranslator = TranslationRepository(
                sourceLanguage = TranslateLanguage.ENGLISH,
                targetLanguage = TranslateLanguage.SPANISH
            )

            try {
                jobTranslator.ensureModelDownloaded(wifiOnly = true)

                chapters.forEachIndexed { index, (spineUrl, rawHtml) ->
                    val translatedHtml = translateChapterHtml(
                        rawHtml = rawHtml,
                        translateText = jobTranslator::translate
                    )
                    translatedFiles[spineUrl] = EpubFile(
                        absPath = spineUrl,
                        data = translatedHtml.toByteArray(Charsets.UTF_8)
                    )
                    onProgress(index + 1, chapters.size)
                }

//                val translatedEpub = writeEpubToFile(
//                    files = translatedFiles,
//                    targetEpubFile = targetEpubFile
//                )

                val translatedEpub = getEpubBytes(
                    files = translatedFiles
                )

                val translatedBookId = repository.addBook(
                    fileBytes = translatedEpub,
                    fileName = "${sourceBook.title} (ES)",
                    customFilename  = "${sourceBook.title} (ES)"
                )

                translatedBookId // return
            } finally {
                jobTranslator.close()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to translate book", e)
            _uiState.update {
                it.copy(error = e.message ?: "Failed to translate book")
            }
            throw e
        }
    }

    private suspend fun translateChapterHtml(
        rawHtml: String,
        translateText: suspend (String) -> String
    ): String {
        val doc = Jsoup.parse(rawHtml, "", Parser.xmlParser())
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false)

        val blocks = doc.select("p, h1, h2, h3, h4, blockquote, li")
        for (el in blocks) {
            if (el.select("p").size > 1) continue

            val originalEn = el.text().trim()
            if (originalEn.isBlank()) continue

            el.attr("data-en", originalEn)
            val translated = translateText(originalEn)
            el.text(translated)
        }

        return doc.outerHtml()
    }



    private fun getEpubBytes(files: Map<String, EpubFile>): ByteArray {
        val byteStream = ByteArrayOutputStream()

        ZipOutputStream(byteStream.buffered()).use { zipOut ->
            // 1. EPUB spec requires "mimetype" to be the very first entry and STORED (uncompressed)
            val mimeBytes = files["mimetype"]?.data
                ?: "application/epub+zip".toByteArray(Charsets.US_ASCII)

            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                crc = CRC32().apply { update(mimeBytes) }.value
            }
            zipOut.putNextEntry(mimeEntry)
            zipOut.write(mimeBytes)
            zipOut.closeEntry()

            // 2. All remaining entries can be DEFLATED (compressed)
            zipOut.setMethod(ZipOutputStream.DEFLATED)
            for ((path, epubFile) in files) {
                if (path == "mimetype") continue
                zipOut.putNextEntry(ZipEntry(path))
                zipOut.write(epubFile.data)
                zipOut.closeEntry()
            }
        }

        return byteStream.toByteArray()
    }

    private fun writeEpubToFile(
        files: Map<String, EpubFile>,
        targetEpubFile: File
    ): File {
        val parentDir = targetEpubFile.parentFile
            ?: throw IllegalStateException("Target EPUB has no parent directory: ${targetEpubFile.path}")
        parentDir.mkdirs()

        val tempFile = File(parentDir, "${targetEpubFile.name}.tmp")

        try {
            ZipOutputStream(tempFile.outputStream().buffered()).use { zipOut ->
                val mimeBytes = files["mimetype"]?.data
                    ?: "application/epub+zip".toByteArray(Charsets.US_ASCII)

                val mimeEntry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimeBytes.size.toLong()
                    compressedSize = mimeBytes.size.toLong()
                    crc = CRC32().apply { update(mimeBytes) }.value
                }
                zipOut.putNextEntry(mimeEntry)
                zipOut.write(mimeBytes)
                zipOut.closeEntry()

                zipOut.setMethod(ZipOutputStream.DEFLATED)
                for ((path, epubFile) in files) {
                    if (path == "mimetype") continue
                    zipOut.putNextEntry(ZipEntry(path))
                    zipOut.write(epubFile.data)
                    zipOut.closeEntry()
                }
            }

            if (targetEpubFile.exists() && !targetEpubFile.delete()) {
                throw IOException("Failed to remove existing file at ${targetEpubFile.absolutePath}")
            }
            if (!tempFile.renameTo(targetEpubFile)) {
                throw IOException("Failed to move ${tempFile.absolutePath} to ${targetEpubFile.absolutePath}")
            }

            return targetEpubFile
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }
    }
}
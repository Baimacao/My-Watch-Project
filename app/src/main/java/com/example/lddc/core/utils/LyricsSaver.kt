package com.example.lddc.core.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.lddc.common.models.enums.LyricsFormat
import java.io.File

object LyricsSaver {

    private val illegalChars = Regex("""[\\/:*?"<>|\r\n\t]""")

    data class SaveResult(
        val uri: Uri?,
        val displayPath: String?,          // null = 失败
        val usedFallback: Boolean = false  // 自定义目录不可用，回退到默认
    )

    fun buildFileName(title: String?, artist: String?, format: LyricsFormat): String {
        val base = listOfNotNull(
            title?.trim()?.takeIf { it.isNotBlank() },
            artist?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" - ").ifBlank { "lyrics" }

        val safe = base.replace(illegalChars, "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)

        return "$safe.${extensionFor(format)}"
    }

    fun extensionFor(format: LyricsFormat): String = when (format) {
        LyricsFormat.VERBATIMLRC,
        LyricsFormat.LINEBYLINELRC,
        LyricsFormat.ENHANCEDLRC,
        LyricsFormat.QRC,
        LyricsFormat.KRC,
        LyricsFormat.YRC -> "lrc"
        LyricsFormat.SRT -> "srt"
        LyricsFormat.ASS -> "ass"
        LyricsFormat.JSON -> "json"
    }

    private fun mimeTypeFor(format: LyricsFormat): String = when (format) {
        LyricsFormat.SRT -> "application/x-subrip"
        LyricsFormat.ASS -> "text/x-ssa"
        LyricsFormat.JSON -> "application/json"
        else -> "text/plain"
    }

    /** SAF createDocument 统一用 octet-stream，避免部分 provider 追加/改写扩展名 */
    private const val SAF_MIME = "application/octet-stream"

    /**
     * @param treeUriString 自定义目录（SAF tree uri）；null/空 → 默认目录
     */
    fun save(
        context: Context,
        fileName: String,
        content: String,
        format: LyricsFormat,
        treeUriString: String? = null
    ): SaveResult {
        // 1. 自定义目录
        if (!treeUriString.isNullOrBlank() && DownloadLocation.isValid(context, treeUriString)) {
            saveToTree(context, Uri.parse(treeUriString), fileName, content, format)?.let {
                val dir = DownloadLocation.displayName(context, treeUriString)
                return SaveResult(it, "$dir/$fileName")
            }
        }

        // 2. 回退默认
        val usedFallback = !treeUriString.isNullOrBlank()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, fileName, content, format)
        } else {
            saveToLegacyExternalStorage(context, fileName, content, format)
        }

        return if (uri != null) {
            SaveResult(uri, "${DownloadLocation.DEFAULT_DISPLAY}/$fileName", usedFallback)
        } else {
            SaveResult(null, null, usedFallback)
        }
    }

    // ---------------- SAF ----------------

    private fun saveToTree(
        context: Context,
        treeUri: Uri,
        fileName: String,
        content: String,
        format: LyricsFormat
    ): Uri? = try {
        val resolver = context.contentResolver
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val finalName = uniqueNameInTree(context, treeUri, parentDocUri, fileName)
        val newUri = DocumentsContract.createDocument(
            resolver, parentDocUri, SAF_MIME, finalName
        ) ?: return null

        resolver.openOutputStream(newUri)?.use {
            it.write(content.toByteArray(Charsets.UTF_8))
        }
        newUri
    } catch (e: Exception) {
        null
    }

    private fun uniqueNameInTree(
        context: Context,
        treeUri: Uri,
        parentDocUri: Uri,
        fileName: String
    ): String {
        val resolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, DocumentsContract.getDocumentId(parentDocUri)
        )
        val existing = mutableSetOf<String>()
        runCatching {
            resolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) c.getString(0)?.let { existing.add(it) }
            }
        }
        if (fileName !in existing) return fileName

        val base = fileName.substringBeforeLast('.', fileName)
        val ext = fileName.substringAfterLast('.', "")
        var i = 1
        while (true) {
            val candidate = if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext"
            if (candidate !in existing) return candidate
            i++
        }
    }

    // ---------------- MediaStore (API 29+) ----------------

    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        content: String,
        format: LyricsFormat
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeTypeFor(format))
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LDDC")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return try {
            val ok = resolver.openOutputStream(uri)?.use {
                it.write(content.toByteArray(Charsets.UTF_8)); true
            } ?: false
            if (!ok) {
                resolver.delete(uri, null, null); return null
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    // ---------------- Legacy (API 24–28) ----------------

    @Suppress("DEPRECATION")
    private fun saveToLegacyExternalStorage(
        context: Context,
        fileName: String,
        content: String,
        format: LyricsFormat
    ): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "LDDC"
        )
        if (!dir.exists() && !dir.mkdirs()) return null

        var file = File(dir, fileName)
        if (file.exists()) {
            val base = fileName.substringBeforeLast('.', fileName)
            val ext = fileName.substringAfterLast('.', "")
            var i = 1
            while (file.exists()) {
                file = File(dir, if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext")
                i++
            }
        }
        return try {
            file.writeText(content, Charsets.UTF_8)
            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf(mimeTypeFor(format)), null
            )
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
}
package com.example.lddc.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object DownloadLocation {

    const val DEFAULT_DISPLAY = "Download/LDDC"

    /** 拿到 SAF 目录后调用，持久化读写权限 */
    fun takePermission(context: Context, uri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        true
    } catch (e: SecurityException) {
        false
    }

    fun releasePermission(context: Context, uriString: String) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    /** 校验 URI 是否仍可用（权限未丢、目录可写） */
    fun isValid(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = Uri.parse(uriString)
        val hasPermission = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        if (!hasPermission) return false
        return DocumentFile.fromTreeUri(context, uri)?.canWrite() == true
    }

    /** 用于设置页 / Toast 展示 */
    fun displayName(context: Context, uriString: String?): String {
        if (uriString.isNullOrBlank()) return DEFAULT_DISPLAY
        val doc = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            ?: return "（已失效）"
        return doc.name ?: DEFAULT_DISPLAY
    }
}
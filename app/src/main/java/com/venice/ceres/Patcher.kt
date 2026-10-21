package com.venice.ceres

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

object Patcher {

    private const val FILE = "UserCustom.ini"
    private const val BAK = "UserCustom.ini.bak"
    private const val CFG = "contentcache/Compulsory/android/gameassetbundles/config"

    fun saveUri(ctx: Context, uri: Uri) {
        ctx.getSharedPreferences("ceres", Context.MODE_PRIVATE)
            .edit().putString("tree", uri.toString()).apply()
    }

    fun loadUri(ctx: Context): Uri? {
        val s = ctx.getSharedPreferences("ceres", Context.MODE_PRIVATE)
            .getString("tree", null) ?: return null
        return runCatching { Uri.parse(s) }.getOrNull()
    }

    private fun childDir(parent: DocumentFile, path: String): DocumentFile? {
        var cur: DocumentFile? = parent
        for (part in path.split("/")) {
            if (part.isBlank()) continue
            val found = cur?.findFile(part)
            cur = if (found != null && found.isDirectory) found else cur?.createDirectory(part)
            if (cur == null) return null
        }
        return cur
    }

    private fun write(ctx: Context, dir: DocumentFile, name: String, content: String): Boolean {
        return try {
            val existing = dir.findFile(name)
            val uri = existing?.uri ?: DocumentsContract.createDocument(
                ctx.contentResolver, dir.uri, "text/plain", name
            )
            if (uri == null) return false
            ctx.contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(content.toByteArray())
            }
            true
        } catch (e: Exception) { false }
    }

    fun apply(ctx: Context, treeUri: Uri, content: String): String {
        val root = DocumentFile.fromTreeUri(ctx, treeUri)
            ?: return "X No se pudo abrir la carpeta"

        val files = childDir(root, "files")
            ?: return "X No se pudo crear la carpeta files"

        val original = files.findFile(FILE)
        if (original != null && files.findFile(BAK) == null) {
            runCatching {
                val bytes = ctx.contentResolver.openInputStream(original.uri)?.use { it.readBytes() }
                if (bytes != null) write(ctx, files, BAK, String(bytes))
            }
        }

        val report = StringBuilder()
        if (write(ctx, files, FILE, content)) {
            report.appendLine("OK -> files/UserCustom.ini")
        } else {
            report.appendLine("X No se pudo escribir en files")
        }

        val cfg = childDir(files, CFG)
        if (cfg != null && write(ctx, cfg, FILE, content)) {
            report.appendLine("OK -> config/UserCustom.ini")
        }

        return report.toString().ifBlank { "Nada que escribir" }
    }
}

package com.venice.ceres

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

object Patcher {

    private const val FILE = "UserCustom.ini"
    private const val BAK = "UserCustom.ini.bak"
    private const val CFG = "contentcache/Compulsory/android/gameassetbundles/config"
    private const val PREFS = "ceres"

    fun saveTree(ctx: Context, uri: Uri) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("tree", uri.toString()).apply()

    fun loadTree(ctx: Context): Uri? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("tree", null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun clearTree(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("tree").apply()

    fun treeName(uri: Uri): String {
        val id = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return "?"
        return id.substringAfterLast('/').ifBlank { id.substringAfterLast(':') }.ifBlank { "?" }
    }

    // ---------- acepta que elijas el juego O su carpeta files ----------

    private fun baseDir(root: DocumentFile): DocumentFile {
        if (root.name == "files") return root
        root.findFile("files")?.let { if (it.isDirectory) return it }
        return dir(root, "files") ?: root
    }

    private fun dir(parent: DocumentFile, path: String): DocumentFile? {
        var cur: DocumentFile? = parent
        for (part in path.split("/")) {
            if (part.isBlank()) continue
            val found = cur?.findFile(part)
            cur = if (found != null && found.isDirectory) found else cur?.createDirectory(part)
            if (cur == null) return null
        }
        return cur
    }

    // ---------- nombre exacto: Android suele pegarle ".txt" ----------

    private fun openExact(ctx: Context, dir: DocumentFile, name: String): Uri? {
        dir.findFile(name)?.let { return it.uri }

        for (extra in listOf(".txt", ".bin", ".dat")) {
            val wrong = dir.findFile(name + extra) ?: continue
            val fixed = runCatching {
                DocumentsContract.renameDocument(ctx.contentResolver, wrong.uri, name)
            }.getOrNull()
            return fixed ?: wrong.uri
        }

        val made = DocumentsContract.createDocument(
            ctx.contentResolver, dir.uri, "application/octet-stream", name
        ) ?: return null

        val real = runCatching { DocumentFile.fromSingleUri(ctx, made)?.name }.getOrNull()
        if (real != null && real != name) {
            val fixed = runCatching {
                DocumentsContract.renameDocument(ctx.contentResolver, made, name)
            }.getOrNull()
            if (fixed != null) return fixed
        }
        return made
    }

    private fun write(ctx: Context, dir: DocumentFile, name: String, text: String): Boolean {
        return try {
            val uri = openExact(ctx, dir, name) ?: return false
            ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readFile(ctx: Context, dir: DocumentFile, name: String): String? {
        return try {
            val f = dir.findFile(name) ?: dir.findFile(name + ".txt") ?: return null
            ctx.contentResolver.openInputStream(f.uri)?.use { String(it.readBytes()) }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- plan B ----------

    private fun legacyWrite(text: String): String {
        val rep = StringBuilder()
        for (pkg in listOf("com.dts.freefireth", "com.dts.freefiremax")) {
            val d = File(Environment.getExternalStorageDirectory(), "Android/data/" + pkg + "/files")
            if (!d.isDirectory && !d.mkdirs() && !d.isDirectory) continue
            try {
                File(d, FILE).writeText(text)
                rep.appendLine("OK (ruta directa) -> " + pkg + "/files")
            } catch (e: Exception) {
            }
        }
        return rep.toString()
    }

    // ---------- acciones ----------

    fun apply(ctx: Context, tree: Uri, text: String): String {
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return "X No se pudo abrir la carpeta"
        val base = baseDir(root)
        val report = StringBuilder()

        val teniaTxt = base.findFile(FILE + ".txt") != null

        if (base.findFile(BAK) == null) {
            val old = readFile(ctx, base, FILE)
            if (old != null && write(ctx, base, BAK, old)) report.appendLine("backup -> " + BAK)
        }

        if (write(ctx, base, FILE, text)) report.appendLine("OK -> " + FILE)
        else report.appendLine("X No se pudo escribir " + FILE)

        // deja tambien la copia con el nombre viejo, por si el juego lee esa
        if (teniaTxt) {
            if (write(ctx, base, FILE + ".txt", text)) report.appendLine("OK -> " + FILE + ".txt")
        }

        val cfg = dir(base, CFG)
        if (cfg != null && write(ctx, cfg, FILE, text)) report.appendLine("OK -> config/" + FILE)

        if (!report.contains("OK ->")) {
            val alt = legacyWrite(text)
            if (alt.isNotBlank()) report.append(alt)
        }
        return report.toString().trim().ifBlank { "Nada que hacer" }
    }

    fun restore(ctx: Context, tree: Uri): String {
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return "X No se pudo abrir la carpeta"
        val base = baseDir(root)
        val bak = readFile(ctx, base, BAK) ?: return "No hay backup"
        return if (write(ctx, base, FILE, bak)) "OK restaurado" else "X No se pudo restaurar"
    }

    fun verify(ctx: Context, tree: Uri): String {
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return "X No se pudo abrir la carpeta"
        val base = baseDir(root)
        val sb = StringBuilder()
        sb.appendLine("Carpeta: " + (root.name ?: "?"))
        val exact = base.findFile(FILE)
        sb.appendLine(FILE + ": " + (if (exact != null) "SI (" + exact.length() + " bytes)" else "no"))
        sb.appendLine(FILE + ".txt: " + (if (base.findFile(FILE + ".txt") != null) "si" else "no"))
        sb.appendLine(BAK + ": " + (if (base.findFile(BAK) != null) "si" else "no"))
        val body = readFile(ctx, base, FILE)
        if (body != null) sb.appendLine("---").append(body.lineSequence().take(4).joinToString("\n"))
        return sb.toString().trim()
    }
}

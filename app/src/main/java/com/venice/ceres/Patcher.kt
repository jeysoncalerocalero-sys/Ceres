package com.venice.ceres

import android.os.Build
import android.os.Environment
import java.io.File

object Patcher {

    private val ROOT = Environment.getExternalStorageDirectory()
    private val PACKAGES = listOf("com.dts.freefireth", "com.dts.freefiremax")

    fun dataDir(pkg: String) = File(ROOT, "Android/data/$pkg")

    private fun targets(pkg: String): List<File> {
        val base = dataDir(pkg)
        return listOf(
            File(base, "files"),
            File(base, "files/contentcache/Compulsory/android/gameassetbundles/config")
        )
    }

    fun installedPackages(): List<String> = PACKAGES.filter { dataDir(it).exists() }

    fun hasAccess(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else true
    } catch (e: Exception) { false }

    private fun ensureDir(dir: File): Boolean {
        if (dir.isDirectory) return true
        val parent = dir.parentFile
        if (parent != null && !parent.isDirectory) ensureDir(parent)
        if (dir.mkdirs()) return true
        if (dir.mkdir()) return true
        return dir.isDirectory
    }

    fun apply(pkg: String, content: String): String {
        val report = StringBuilder()
        for (dir in targets(pkg)) {
            if (!ensureDir(dir)) {
                report.appendLine("X No se pudo crear: ${dir.path}")
                continue
            }
            val target = File(dir, "UserCustom.ini")
            val bak = File(dir, "UserCustom.ini.bak")
            if (target.exists() && !bak.exists()) target.copyTo(bak, overwrite = true)
            target.writeText(content)
            report.appendLine("OK -> ${target.path}")
        }
        return report.toString().ifBlank { "Nada que escribir" }
    }

    fun restore(pkg: String): String {
        val report = StringBuilder()
        for (dir in targets(pkg)) {
            val bak = File(dir, "UserCustom.ini.bak")
            val target = File(dir, "UserCustom.ini")
            if (bak.exists()) {
                bak.copyTo(target, overwrite = true)
                report.appendLine("OK restaurado -> ${target.path}")
            }
        }
        return report.toString().ifBlank { "No hay backup para restaurar" }
    }
}

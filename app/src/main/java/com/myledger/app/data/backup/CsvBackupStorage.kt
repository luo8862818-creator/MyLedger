package com.myledger.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object CsvBackupStorage {
    const val AutomaticBackupFileName = "我的账单_自动备份.csv"

    /** Writes the latest complete backup into a user-selected document-tree folder. */
    fun writeAutomaticBackup(context: Context, treeUri: Uri, csv: String): Boolean {
        val directory = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        if (!directory.canWrite()) return false
        val file = directory.findFile(AutomaticBackupFileName)
            ?: directory.createFile("text/csv", AutomaticBackupFileName)
            ?: return false
        context.contentResolver.openOutputStream(file.uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(csv)
        } ?: return false
        return true
    }
}

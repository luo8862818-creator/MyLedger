package com.myledger.app.data.backup

import android.content.Context

class BackupPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("backup_preferences", Context.MODE_PRIVATE)

    var treeUri: String?
        get() = preferences.getString(TreeUriKey, null)
        set(value) = preferences.edit().putString(TreeUriKey, value).apply()

    var lastBackupAt: Long
        get() = preferences.getLong(LastBackupAtKey, 0L)
        set(value) = preferences.edit().putLong(LastBackupAtKey, value).apply()

    fun disable() {
        preferences.edit().remove(TreeUriKey).remove(LastBackupAtKey).apply()
    }

    private companion object {
        const val TreeUriKey = "tree_uri"
        const val LastBackupAtKey = "last_backup_at"
    }
}

package com.example.voicecatch_ver2.file

import android.net.Uri

sealed class FileListItem {
    data class FileItem(val name: String, val uri: Uri, val size: String, val date: String) : FileListItem()
    object EmptyItem : FileListItem()
}
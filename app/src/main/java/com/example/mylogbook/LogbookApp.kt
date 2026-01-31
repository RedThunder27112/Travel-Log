package com.example.mylogbook

import android.app.Application
import android.net.Uri
import com.example.mylogbook.data.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LogbookApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    private val _sharedImportUri = MutableStateFlow<Uri?>(null)
    val sharedImportUri: StateFlow<Uri?> = _sharedImportUri

    override fun onCreate() {
        super.onCreate()
    }

    fun setSharedImport(uri: Uri) {
        _sharedImportUri.value = uri
    }

    fun clearSharedImport() {
        _sharedImportUri.value = null
    }
}

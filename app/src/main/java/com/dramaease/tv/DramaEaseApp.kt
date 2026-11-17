package com.dramaease.tv

import android.app.Application
import androidx.room.Room
import com.dramaease.tv.data.AppDatabase
import com.dramaease.tv.data.Library
import com.dramaease.tv.data.SettingsRepository
import com.dramaease.tv.provider.ContentSource
import com.dramaease.tv.provider.DemoContentProvider
import com.dramaease.tv.provider.KissKhContentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = Room.databaseBuilder(app, AppDatabase::class.java, "dramaease.db").build()
    val settings = SettingsRepository(app)

    /** adb push kisskh_library.json /sdcard/Android/data/com.dramaease.tv/files/ */
    val kisskhLibraryFile: File? = app.getExternalFilesDir(null)?.let { File(it, KISSKH_LIBRARY_FILE) }

    val library = Library(
        dao = db.userDao(),
        settings = settings,
        providers = mapOf(
            ContentSource.DEMO to DemoContentProvider(),
            ContentSource.KISSKH to KissKhContentProvider(kisskhLibraryFile),
        ),
        scope = appScope,
    )

    companion object { const val KISSKH_LIBRARY_FILE = "kisskh_library.json" }
}

class DramaEaseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

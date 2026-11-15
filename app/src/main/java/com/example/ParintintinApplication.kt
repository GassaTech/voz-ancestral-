package com.example

import android.app.Application
import com.example.data.local.seed.ArchiveSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ParintintinApplication : Application() {

    @Inject
    lateinit var archiveSeeder: ArchiveSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            archiveSeeder.ensureSeeded()
        }
    }
}

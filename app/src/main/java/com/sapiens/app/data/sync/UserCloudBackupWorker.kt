package com.sapiens.app.data.sync

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserCloudBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Result.success()
        return@withContext try {
            val repo = UserCloudBackupRepository.create(applicationContext as Application)
            repo.performBackup(uid)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

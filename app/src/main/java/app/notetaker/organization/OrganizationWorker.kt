package app.notetaker.organization

import android.content.Context
import androidx.work.*
import app.notetaker.DefaultAppContainer
import app.notetaker.NoteTakerApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class OrganizationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val app = applicationContext as? NoteTakerApplication ?: return@withContext Result.success()
        val container = app.container as DefaultAppContainer
        try {
            val queued = container.database.organizationDao().state()
            if (queued == null || queued.completed == queued.generation) return@withContext Result.success()
            LocalTextEmbedding(applicationContext).use { model ->
                // Bound each run; unfinished generations remain durably queued.
                repeat(3) { if (NotebookOrganizer(container.database, model).runOnce()) return@withContext Result.success() }
            }
            Result.retry()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            container.database.organizationDao().failure("Automatic organization is delayed. Tap Retry to try again.")
            Result.retry()
        }
    }
    companion object {
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<OrganizationWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork("organize-notebook", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}

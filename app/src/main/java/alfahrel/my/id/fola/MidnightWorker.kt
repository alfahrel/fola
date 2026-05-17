package alfahrel.my.id.fola

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MidnightWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // Notify the app that the date has changed
        applicationContext.sendBroadcast(
            android.content.Intent(ACTION_DATE_CHANGED)
        )

        // Schedule the next midnight trigger
        scheduleMidnightWork(applicationContext)

        return Result.success()
    }

    companion object {
        const val ACTION_DATE_CHANGED = "alfahrel.my.id.kalender.DATE_CHANGED"
        private const val WORK_NAME = "midnight_refresh"

        fun scheduleMidnightWork(context: Context) {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delay = midnight.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<MidnightWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
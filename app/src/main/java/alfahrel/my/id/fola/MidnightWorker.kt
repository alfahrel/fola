package alfahrel.my.id.fola

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import alfahrel.my.id.fola.data.FolaDatabase
import alfahrel.my.id.fola.data.model.RepeatType
import alfahrel.my.id.fola.data.model.Task
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MidnightWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        processRepeatTasks()

        applicationContext.sendBroadcast(
            android.content.Intent(ACTION_DATE_CHANGED)
        )

        scheduleMidnightWork(applicationContext)
        return Result.success()
    }

    private fun processRepeatTasks() {
        val dao = FolaDatabase.getInstance(applicationContext).taskDao()
        val now = System.currentTimeMillis()

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Handle completed repeat tasks — reset them for next occurrence
        dao.getAllCompletedTasksSync()
            .filter { it.isRepeat }
            .forEach { task ->
                val nextDue = advanceDueDate(task)
                dao.updateSync(
                    task.copy(
                        isCompleted = false,
                        dueDateMs   = nextDue,
                        streak      = task.streak + 1
                    )
                )
            }

        // Handle missed repeat tasks (not completed, due date has passed)
        // Reset streak to 0 and advance due date so it shows for today
        dao.getAllActiveTasksSync()
            .filter { it.isRepeat && it.dueDateMs != null && it.dueDateMs < startOfToday }
            .forEach { task ->
                val nextDue = advanceToToday(task, startOfToday)
                dao.updateSync(
                    task.copy(
                        dueDateMs = nextDue,
                        streak    = 0           // missed = streak broken
                    )
                )
            }
    }

    private fun advanceDueDate(task: Task): Long {
        val base = task.dueDateMs ?: System.currentTimeMillis()
        val cal  = Calendar.getInstance().apply { timeInMillis = base }

        when (task.repeatType) {
            RepeatType.DAILY   -> cal.add(Calendar.DAY_OF_MONTH, task.repeatInterval)
            RepeatType.WEEKLY  -> cal.add(Calendar.WEEK_OF_YEAR, task.repeatInterval)
            RepeatType.MONTHLY -> cal.add(Calendar.MONTH,        task.repeatInterval)
            RepeatType.CUSTOM  -> cal.add(Calendar.DAY_OF_MONTH, task.repeatInterval)
        }

        return cal.timeInMillis
    }

    private fun advanceToToday(task: Task, startOfToday: Long): Long {
        var next = task.dueDateMs ?: startOfToday
        while (next < startOfToday) {
            val cal = Calendar.getInstance().apply { timeInMillis = next }
            when (task.repeatType) {
                RepeatType.DAILY   -> cal.add(Calendar.DAY_OF_MONTH, task.repeatInterval)
                RepeatType.WEEKLY  -> cal.add(Calendar.WEEK_OF_YEAR, task.repeatInterval)
                RepeatType.MONTHLY -> cal.add(Calendar.MONTH,        task.repeatInterval)
                RepeatType.CUSTOM  -> cal.add(Calendar.DAY_OF_MONTH, task.repeatInterval)
            }
            next = cal.timeInMillis
        }
        return next
    }

    companion object {
        const val ACTION_DATE_CHANGED = "alfahrel.my.id.kalender.DATE_CHANGED"
        private const val WORK_NAME   = "midnight_refresh"

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
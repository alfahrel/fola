package alfahrel.my.id.fola.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Task",
    val startDateMs: Long? = null,
    val dueDateMs: Long? = null,
    val isCompleted: Boolean = false,
    val isRepeat: Boolean = false,
    val repeatType: RepeatType = RepeatType.DAILY,
    val repeatInterval: Int = 1,
    val repeatUnit: String = "days",
    val streak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RepeatType {
    DAILY, WEEKLY, MONTHLY, CUSTOM
}
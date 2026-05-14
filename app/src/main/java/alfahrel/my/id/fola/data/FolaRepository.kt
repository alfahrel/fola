package alfahrel.my.id.fola.data

import alfahrel.my.id.fola.data.model.Task

class FolaRepository(private val db: FolaDatabase) {

    private val dao = db.taskDao()

    val activeTasks    = dao.getActiveTasks()
    val completedTasks = dao.getCompletedTasks()

    suspend fun insertTask(task: Task) = dao.insert(task)

    suspend fun updateTask(task: Task) = dao.update(task)

    suspend fun deleteTask(task: Task) = dao.delete(task)

    suspend fun setTaskCompleted(id: Long, completed: Boolean) = dao.setCompleted(id, completed)
}
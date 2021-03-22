package com.example.doDue.data

import androidx.room.*
import com.example.doDue.ui.tasks.SortOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /* suspend = puts the process/function on a background thread, instead of it working on the main UI thread.
    * If not used, this would still perform the operation, but the UI would halt for the time it takes to perform the DB operation (which is obviously not good).
    * suspend can only be called using another suspend func. or a Coroutine. */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    /* onConflict is just a metadata, which defines what should happen when two same entries are about to be added to the database. Here, we REPLACE them. */

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    /* SQL queries written in room are compile time safe, unlike SQLite where it was caught only in runtime. */

    /* Flow is an async stream of data that will be received. That is, if any data is updated later on and then this function is called, it will return a stream of
    * current data as a Flow. Here it will be a Flow of List of Task.
    * Flow can only be used inside a Coroutine, therefore we don't need a suspend modifier here (as Coroutine handles that automatically). */

    /* || means append in SQLite (not OR). */

    /* We can pass a SortOrder type value to the getTasks function, but our Database doesn't have any entries/columns that correspond to it. Therefore, we have to
    * create two separate functions and have to hardcode the Sort by name and Sort by date functionaries.
    * Also we have to create a third separate function, that actually takes in all the 3 parameters and then can call these two functions. */

    /* (completed != :hideCompletedTasks OR completed = 0) means that if hideCompletedTasks is true, we want to make completed as false, i.e. show only
    * uncompleted tasks. But if hideCompTasks is false, then we actually have to show both completed and uncompleted tasks. completed actually becomes true then,
    * but completed = 0 still shows uncompleted tasks. */

    fun getTasks(query: String, sortOrder: SortOrder, hideCompletedTasks: Boolean): Flow<List<Task>> =
        when(sortOrder){
            SortOrder.BY_DATE -> getTasksByDateCreated(query, hideCompletedTasks)
            SortOrder.BY_NAME -> getTasksByName(query, hideCompletedTasks)
        }

    @Query("SELECT * FROM task_table WHERE (completed != :hideCompletedTasks OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY priority DESC, name")
    fun getTasksByName(searchQuery: String, hideCompletedTasks: Boolean): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE (completed != :hideCompletedTasks OR completed = 0) AND name LIKE '%' || :searchQuery || '%' ORDER BY priority DESC, date_time_created")
    fun getTasksByDateCreated(searchQuery: String, hideCompletedTasks: Boolean): Flow<List<Task>>


    // Final query to delete all completed tasks.

    @Query("DELETE FROM task_table WHERE completed = 1")
    suspend fun deleteCompletedTasks()

}
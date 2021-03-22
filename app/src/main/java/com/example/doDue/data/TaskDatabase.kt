package com.example.doDue.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.doDue.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    /* We create an abstract function here as we want to call the functions/operations through DAO and not directly (to provide abstraction). */

    /* Also, we would use Dependency Injection to call the DAO Interface only wherever we need it. This is because our class (Database class here)
    * shouldn't be responsible for creating or searching the object/class needed (DAO Interface in this case).
    *
    * Therefore, Dagger is used to create containers of such big objects/classes/interfaces and provide it to us, wherever and whenever we need it in our code.
    * Hilt (a library) is used to make the use of Dagger easier.  */

    /* We want to have some elements added to our list when the app starts, so we need our Database to have those entries in advance. Therefore, we define
    * them here. But this also needs to be available in the TaskDatabase Provider, as it will provide this database wherever necessary within our app.
    * So we make a 'callback' in the TaskDatabase Provider part of the AppModule object, for which we created this Callback class here
    * having the initial list elements. */

    /* @Inject works the same like @Provides. But @Provides is used where the definition is not user-defined but instead is present in a library.
    * @Inject is(and should be) used where anything defined inside it is defined by the user itself and no library criterion is there. */

    /* What is happening here is we need a DAO reference to add tasks. DAO needs Database reference to get created. And Database needs to call this Callback
    * which in turn needs to create a DAO. Thus a loop is created.
    * This is possible as the trick is that callback function works first and then only the Database with the entries will be created later. */

    /* But again to create a DAO here, we need a Database reference. We can pass it as a constructor variable,
     but a normal TaskDatabase would again mean a circular dependency. Therefore, we pass it as a Provider of TaskDatabase (i.e. Provider<TaskDatabase>. */
    /* This in a way, just lazily provides the definition of the TaskDatabase(and its functions to use) instead of initializing and making it. */

    /* ApplicationScope is the self-made annotation(made in AppModule) for precise marking of Coroutine scope. */

    class Callback @Inject constructor(private val database: Provider<TaskDatabase>, @AppModule.ApplicationScope private val applicationScope: CoroutineScope) :
        RoomDatabase.Callback() {

        /* onCreate function here is called when the Database is made for the first time and not every time when we open the app,
        which is the correct time to make the initial list of items. */

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            // Provider<TaskDatabase> only gave the lazy definition of the DB and nothing was initialized. get() function here is used to do so.

            val dao = database.get().taskDao()

            /* But all the operations of DAO are suspend operations. Therefore to run them, we need a Coroutine(lightweight thread),as suspend operations
            run separately on another thread. Also, every Coroutine needs a scope to work in, Therefore, we create a scope in the Dagger Module. */

            applicationScope.launch {
                dao.insert(Task("Wash the dishes"))
                dao.insert(Task("Do the Laundry", completed = true))
                dao.insert(Task("Buy Groceries", priority = true))
                dao.insert(Task("Repair Bike"))
                dao.insert(Task("Call Grandma", completed = true))
            }

        }

    }

}
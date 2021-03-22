package com.example.doDue.di

import android.app.Application
import androidx.room.Room
import com.example.doDue.data.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/* A Module is a Dagger container where we define what and how we want to use that particular object later somewhere in our code (for Dependency Injection). */

@Module
@InstallIn(ApplicationComponent::class)

/* ApplicationComponent contains the dependencies that we want to use throughout our whole application. Therefore we tell it to @InstallIn this Module inside it.
* As we want to use this Module and thus the same database throughout the app. */

object AppModule {

    /* provides is a way to write the name of the feature/object to be provided by Dagger.
    Also @Provides is also an annotation for telling that the function is nothing but a kind of an instruction to provide that object to another code part. */

    /* @Singleton is used as we would only need one instance of our Database and DAO(it's done here automatically) at any given time in our app. */

    /* fallbackToDestructiveMigration destroys the table and creates a new one in case a proper migration strategy is not followed. */

    /* Callback is made to another class (CallBack class) in the TaskDatabase file as this provides for the entries we need to have for every first time
    the app is run. Those entries (and thus the Callback class) is made there because of separation of concerns.*/

    @Provides
    @Singleton
    fun provideDatabase(app: Application, callback: TaskDatabase.Callback) = Room.databaseBuilder(app, TaskDatabase::class.java, "tasks-db").fallbackToDestructiveMigration().addCallback(callback).build()

    /* The previous method is used to provide a TaskDatabase. This method needs a TaskDatabase to provide a TaskDao.
    So, we make use of it in the function arguments. */

    @Provides
    fun provideTaskDao(db: TaskDatabase) = db.taskDao()

    /* Provides the scope for the Coroutine that works in the Callback class of TaskDatabase. */
    /* CoroutineScope() provides the scope and SupervisorJob() tells the Coroutine thread
    to not kill other child processes (in the same scope) if one of them (which comes ahead of them) fails. */

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

    /* This is used to make ApplicationScope an annotation. As we may need more than one scope later, we annotate them for precise referencing in other files. */

    @Retention(AnnotationRetention.RUNTIME)
    @Qualifier
    annotation class ApplicationScope

}
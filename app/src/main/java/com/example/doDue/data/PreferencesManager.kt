package com.example.doDue.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.emptyPreferences
import androidx.datastore.preferences.preferencesKey
import com.example.doDue.ui.tasks.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/* This class/file has been created to store our filter preferences which will normally get reset (if we don't store them somewhere) after we close/pause the app.
* For this functionality to be added, we use JetPack DataStore.
*
* JetPack DataStore is also used for storage just like Room, but we use Room(i.e. SQLite) for very large tables and databases.
* For smaller, single answered/typed values, SQLite would be a waste, and DataStore will be best. Also it works not on the UI thread but in bg threads,
* by again, using Flows and Coroutines.
*
* We create this file as a dependency which can later be Injected at the ViewModel of the Fragment, thus providing these values always Live.
*
* dataStore.data is automatically a Flow of type 'Preferences', which is a data type in the DataStore library.
*
* But before we pass it directly to the ViewModel, we will alter it as per our need, to avoid doing it in the ViewModel. */

private const val TAG = "PreferencesManager"

data class FilterPreferences(val sortOrder: SortOrder, val hideCompleted: Boolean)

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.createDataStore("user_preferences")

    val preferencesFlow = dataStore.data.catch { exception ->

        /* Exception Handling and Logging of error message using LogTags.
        * emit(emptyPreferences) will emit any preferences set and will use the default preferences values. */

        if(exception is IOException){
            Log.e(TAG, "ERROR READING PREFERENCES", exception)
            emit(emptyPreferences())
        }
        else{
            throw exception
        }

    }.map {

        /* map{} allows us to work on individual items of an object/container and then modify them up to our needs.
        Here we take out the preferences from the DataStore and then wrap them into FilterPreferences data class and save it to this variable.
        This can be done using map{} only. Now we can use this preferencesFlow variable directly in our ViewModel. */

        /* Here we take out the preference values saved in the DataStore, which we then pass to the ViewModel.
        Elvis operator ensures we don't pass null values and thus we also have to provide the default values to these preferences.
        But we will also convert it into a single object type as output can only be single object. That's why we make the "FilterPreferences" data class. */

        /* Here we are checking by comparing the String value of our saved preference to the String value of the
        SortOrder enum (for which we used .valueOf() to convert enum into String. Similarly for the hideCompleted. */

        /* DEFAULT VALUES OF PREFERENCES SHOULD BE PROVIDED BECAUSE IF NOT THEN THE LIST OF ITEMS WOULDN'T KNOW HOW TO GET ARRANGED INITIALLY.
        * THAT IS WHY WHEN WE TAKE OUT SOMETHING FROM DATASTORE PREFERENCES IT STILL ASKS FOR A DEFAULT VALUE (AS IT USES ? OPERATOR).
        * THIS IS HOW WE ARE GIVING OUR DEFAULT PREFERENCES VALUES HERE, WITHOUT EVEN SAVING THEM INITIALLY. */

        val sortOrder = SortOrder.valueOf(
            it[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name
        )

        val hideCompleted = it[PreferencesKeys.HIDE_COMPLETED_TASKS] ?: false

        FilterPreferences(sortOrder, hideCompleted)

    }

    /* The above function just helps retrieve the data stored in the DataStore which can then be forwarded to the ViewModel.
    * But when the values of those preferences update, then we have to add functionality for that too.
    * Also, again we use suspend functions so as to do these operations in async. */

    suspend fun updateSortOrder(sortOrder: SortOrder){
        dataStore.edit {
            it[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateHideCompleted(hideCompleted: Boolean){
        dataStore.edit {
            it[PreferencesKeys.HIDE_COMPLETED_TASKS] = hideCompleted
        }
    }

    /* PreferencesKeys is just a namespace to define how we can access the different preferences from the DataStore. It's just a naming method, which makes
    accessing objects from the DataStore more readable.
    * Also, PreferencesKeys is only able to store primitive/basic type of data. Therefore, sortOrder has to be converted into String here. And vice-versa when
    need it outside the PreferencesKeys object.*/

    private object PreferencesKeys {
        val SORT_ORDER = preferencesKey<String>("sort_order")
        val HIDE_COMPLETED_TASKS = preferencesKey<Boolean>("hide_completed")
    }

}
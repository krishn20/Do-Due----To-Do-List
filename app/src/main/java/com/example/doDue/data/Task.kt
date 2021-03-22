package com.example.doDue.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.text.DateFormat

@Entity(tableName = "task_table")
@Parcelize
data class Task(val name: String,
                var completed: Boolean = false,
                var priority: Boolean = false,
                var date_time_created: Long = System.currentTimeMillis(),
                @PrimaryKey(autoGenerate = true) val id: Int = 0) : Parcelable
{
    /* As we want to get both Date and Time, we use a constructor value for finding the current system time, and then convert it using DateFormat.
    Also since this won't be a fixed value, we use the get() method of variable initialization for getting a dynamic value. */

    val createdDateTimeFormat: String get() = DateFormat.getDateTimeInstance().format(date_time_created)
}

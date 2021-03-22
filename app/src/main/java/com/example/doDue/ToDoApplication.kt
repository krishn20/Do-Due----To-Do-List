package com.example.doDue

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

//This setup is important to just activate DaggerHilt and thus provide Dependency Injection features all over the app.

@HiltAndroidApp
class ToDoApplication : Application() {
}
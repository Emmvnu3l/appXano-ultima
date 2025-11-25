package com.example.myapplication

import android.app.Application
import android.util.Log
import android.widget.Toast
import android.os.Looper
import android.os.Handler

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppCrash", "Uncaught exception in thread ${thread.name}", throwable)
            
            // Intenta mostrar un Toast en el hilo principal antes de cerrar (si es posible)
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Error inesperado. Reiniciando...", Toast.LENGTH_LONG).show()
                }
            } else {
                try {
                    Toast.makeText(this, "Error inesperado. Reiniciando...", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    // Ignorar si no se puede mostrar
                }
            }

            // Delegar al handler por defecto (que cerrará la app)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
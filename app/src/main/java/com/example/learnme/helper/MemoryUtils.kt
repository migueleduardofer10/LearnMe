package com.tuempresa.helpers

import android.app.ActivityManager
import android.content.Context
import android.util.Log

fun logMemoryUsage(context: Context) {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memoryInfo)

    val availableMemory = memoryInfo.availMem / (1024 * 1024)  // Convertir de bytes a MB
    val totalMemory = memoryInfo.totalMem / (1024 * 1024)  // Convertir de bytes a MB
    val usedMemory = totalMemory - availableMemory

    Log.d("ResourceMonitor", "Memoria total: $totalMemory MB")
    Log.d("ResourceMonitor", "Memoria disponible: $availableMemory MB")
    Log.d("ResourceMonitor", "Memoria usada: $usedMemory MB")
}

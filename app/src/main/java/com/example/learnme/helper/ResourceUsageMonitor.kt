package com.tuempresa.helpers

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.util.Log

class ResourceUsageMonitor(private val context: Context, private val intervalMs: Long = 1000) {
    private val handler = Handler()
    private var cpuUsageList = mutableListOf<Float>()
    private var maxCpuUsage = 0f

    fun startCpuMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                Log.d("ResourceMonitor", "Iniciando la medición de CPU")  // Log de inicio de medición
                val cpuUsage = getCpuUsage()
                cpuUsageList.add(cpuUsage)
                maxCpuUsage = maxOf(maxCpuUsage, cpuUsage)

                Log.d("ResourceMonitor", "Uso actual de CPU: $cpuUsage%")

                handler.postDelayed(this, intervalMs)
            }
        })
    }

    fun stopCpuMonitoring() {
        handler.removeCallbacksAndMessages(null)

        val averageCpuUsage = if (cpuUsageList.isNotEmpty()) {
            cpuUsageList.sum() / cpuUsageList.size
        } else {
            0f
        }

        Log.d("ResourceMonitor", "Uso promedio de CPU: $averageCpuUsage%")
        Log.d("ResourceMonitor", "Uso máximo de CPU: $maxCpuUsage%")
    }

    // Aproximación del uso de la CPU utilizando los procesos activos
    private fun getCpuUsage(): Float {
        try {
            // Usamos ActivityManager para obtener la lista de procesos activos
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses

            // Total de procesos activos
            val totalProcesses = runningAppProcesses.size

            // Si no hay procesos activos, no se puede calcular el uso
            if (totalProcesses == 0) {
                Log.e("ResourceMonitor", "No hay procesos activos para calcular el uso de la CPU.")
                return 0f
            }

            // En este ejemplo, usaremos la cantidad de procesos activos como un indicador de carga
            // Aunque no es una medición exacta del uso de la CPU, nos puede dar una estimación
            val cpuUsage = (totalProcesses.toFloat() / 100)  // Estimación sencilla

            Log.d("ResourceMonitor", "Uso estimado de CPU basado en procesos activos: $cpuUsage%")
            return cpuUsage
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Error al obtener el uso de CPU: ${e.message}", e)  // Log del error
        }
        return 0f
    }
}

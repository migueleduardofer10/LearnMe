package com.tuempresa.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class BatteryUsageMonitor(private val context: Context) {
    private var initialBatteryLevel: Int = 0
    private var finalBatteryLevel: Int = 0

    fun startMonitoring() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        initialBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        Log.d("ResourceMonitor", "Nivel inicial de batería: $initialBatteryLevel%")
    }

    fun stopMonitoring() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        finalBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0

        val batteryConsumption = initialBatteryLevel - finalBatteryLevel
        Log.d("ResourceMonitor", "Nivel final de batería: $finalBatteryLevel%")
        Log.d("ResourceMonitor", "Consumo total de batería: $batteryConsumption%")
    }
}

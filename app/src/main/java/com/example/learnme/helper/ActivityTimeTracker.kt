import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class ActivityTimeTracker {

    private var startTime: Long = 0
    private var endTime: Long = 0

    fun startActivity() {
        startTime = System.currentTimeMillis()
        Log.d("ActivityTimeTracker", "Inicio del Caso de Prueba: ${formatTime(startTime)}")
    }

    fun endActivity() {
        endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        Log.d("ActivityTimeTracker", "Fin del Caso de Prueba: ${formatTime(endTime)}")
        Log.d("ActivityTimeTracker", "Duración total: ${formatDuration(duration)} minutos")
    }

    private fun formatTime(timeInMillis: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timeInMillis))
    }

    private fun formatDuration(durationInMillis: Long): String {
        val minutes = (durationInMillis / 1000) / 60  // Minutos completos
        val seconds = (durationInMillis / 1000) % 60  // Segundos restantes

        return if (minutes > 0) {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        } else {
            String.format(Locale.US, "%02d segundos", seconds)
        }
    }
}

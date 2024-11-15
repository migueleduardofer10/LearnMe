package com.example.learnme.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class CameraPermissionsManager(
    private val context: Context,
    private val onPermissionGranted: () -> Unit
) {
    private val permissionLauncher: ActivityResultLauncher<String> =
        (context as ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionsDenied()
            }
        }

    fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun onPermissionsDenied() {
        Toast.makeText(
            context,
            "Permiso de cámara denegado. No se puede acceder a la cámara.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

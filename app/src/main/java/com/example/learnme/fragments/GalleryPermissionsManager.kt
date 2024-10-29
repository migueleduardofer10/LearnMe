package com.example.learnme.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class GalleryPermissionsManager(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ utiliza permisos específicos para los medios
            val imagePermission = Manifest.permission.READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(context, imagePermission) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                permissionLauncher.launch(imagePermission)
            }
        } else {
            // Versiones anteriores de Android usan READ_EXTERNAL_STORAGE
            val externalStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, externalStoragePermission) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                permissionLauncher.launch(externalStoragePermission)
            }
        }
    }

    private fun onPermissionsDenied() {
        Toast.makeText(
            context,
            "Permission denied. Unable to access the gallery.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
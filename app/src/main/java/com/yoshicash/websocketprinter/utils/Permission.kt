package com.yoshicash.websocketprinter.utils
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

fun requestLocalizationPermissions(activity: Activity) {
    val localizationPermission =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
    val listPermissionsNeeded: MutableList<String> = ArrayList()
    if (localizationPermission != PackageManager.PERMISSION_GRANTED) {
        listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (listPermissionsNeeded.isNotEmpty()) {
        activity.requestPermissions(listPermissionsNeeded.toTypedArray(), 105)
        return
    }
}

fun requestBluetoothPermissions(activity: Activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
            activity, Manifest.permission.BLUETOOTH
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.BLUETOOTH),
            101
        )
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
            activity, Manifest.permission.BLUETOOTH_ADMIN
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
            102
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
            activity, Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            103
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
            activity, Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
            104
        )
    } else {
        Log.d("Bluetooth", "Permissions already granted.")
    }
}
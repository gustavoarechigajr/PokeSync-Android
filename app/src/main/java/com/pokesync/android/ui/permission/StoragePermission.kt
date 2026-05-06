package com.pokesync.android.ui.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Returns true if the app already has unrestricted file access.
 * On Android < 11 this is always true (READ_EXTERNAL_STORAGE is enough).
 */
fun hasAllFilesAccess(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        Environment.isExternalStorageManager()
    else
        true

/**
 * Shows a rationale dialog and then sends the user to the system settings
 * to grant MANAGE_EXTERNAL_STORAGE.
 *
 * Call this from the Saves screen when [hasAllFilesAccess] returns false.
 */
@Composable
fun RequestAllFilesAccessDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Access Needed") },
        text = {
            Text(
                "PokeSync needs access to all files on your device so it can read save files " +
                "from emulators like Eden (which store data in restricted directories).\n\n" +
                "Tap \"Grant Access\" and enable \"Allow access to manage all files\" for PokeSync."
            )
        },
        confirmButton = {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                }
                onDismiss()
            }) { Text("Grant Access") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Not Now") }
        },
    )
}

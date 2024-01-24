package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File

@Composable
fun ExportScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(mediumPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Export data to local files")
            Button(
                onClick = {
                    val path = context.getDatabasePath("response_database")
                    shareDatabase(context,"response_database", "com.jianzheng.studyzero.provider")
                    Log.d("exportData", "path: $path")
                }
            ) {
                Text("Export")
            }
            Button(
                onClick = {
                    navController.navigate(AppRoutes.HOME)
                },
            ) {
                Text("Back")
            }
        }
    }
}

fun shareDatabase(context: Context, databaseName: String, authority: String) {
    // Original database file
    val originalFile = File(context.getDatabasePath(databaseName).path)

    // New file location in external files directory
    val newFile = File(context.getExternalFilesDir(null), databaseName)

    // Copy the database file to the external files directory
    originalFile.copyTo(newFile, overwrite = true)

    // Get URI for the new file
    val fileUri = FileProvider.getUriForFile(context, authority, newFile)

    // Intent to share the file
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Database"))
}

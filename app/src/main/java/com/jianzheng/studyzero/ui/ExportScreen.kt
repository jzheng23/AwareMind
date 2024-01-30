package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
                    shareDatabases(
                        context,
                        listOf("response_database", "input_database", "usage_database"),
                        "com.jianzheng.studyzero.provider"
                    )
//                    shareDatabaseBackup(context, "response_database", "com.jianzheng.studyzero.provider")

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


fun shareDatabases(context: Context, databaseNames: List<String>, authority: String) {
    CoroutineScope(Dispatchers.Main).launch {
        val zipFile = File(context.getExternalFilesDir(null), "databases.zip")
        withContext(Dispatchers.IO) {
            val files = databaseNames.map { dbName ->
                val originalFile = File(context.getDatabasePath(dbName).path)
                val newFile = File(context.getExternalFilesDir(null), "$dbName.db")
                originalFile.copyTo(newFile, overwrite = true)
                newFile
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                files.forEach { file ->
                    FileInputStream(file).use { fis ->
                        BufferedInputStream(fis).use { bis ->
                            val zipEntry = ZipEntry(file.name)
                            zos.putNextEntry(zipEntry)
                            bis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
        }

        // The UI operation remains on the Main thread
        val fileUri = FileProvider.getUriForFile(context, authority, zipFile)

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("jzheng23@umd.edu"))
            putExtra(Intent.EXTRA_SUBJECT, "Study Zero: Database Files")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Verify that the intent will resolve to an activity
        if (emailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(emailIntent, "Send Database via Email"))
        }
    }
}


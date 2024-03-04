package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val responseExist = checkExist(context, "response")
    val inputExist = checkExist(context, "input")
    val usageExist = checkExist(context, "usage")
    var responseToBeExport by remember { mutableStateOf(false) }
    var inputToBeExport by remember { mutableStateOf(false) }
    var usageToBeExport by remember { mutableStateOf(false) }
    //    val databaseNames = listOf("response_database", "input_database", "usage_database")
    val databaseNames = mutableListOf<String>()
//    "response_database", "input_database", "usage_database")
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Data files") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(AppRoutes.HOME)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "back",
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )

        },
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = mediumPadding),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArrowCard(
                        title = "Export",
                        desc = "Export the ",
                        onAuthClick = {
                            shareDatabases(
                                context,
                                databaseNames
                            )
                        }
                    )
                    ChecklistItem(
                        item = "response",
                        checked = responseToBeExport,
                        onCheckedChange = { isChecked ->
                            // Update the external variable
                            responseToBeExport = isChecked
                            if (isChecked) {
                                databaseNames.add("response_database")
                            } else {
                                databaseNames.remove("response_database")
                            }
                        },
                        enabled = responseExist
                    )
                    ChecklistItem(
                        item = "input",
                        checked = inputToBeExport,
                        onCheckedChange = { isChecked ->
                            // Update the external variable
                            inputToBeExport = isChecked
                            if (isChecked) {
                                databaseNames.add("input_database")
                            } else {
                                databaseNames.remove("input_database")
                            }
                        },
                        enabled = inputExist
                    )
                    ChecklistItem(
                        item = "usage",
                        checked = usageToBeExport,
                        onCheckedChange = { isChecked ->
                            // Update the external variable
                            usageToBeExport = isChecked
                            if (isChecked) {
                                databaseNames.add("usage_database")
                            } else {
                                databaseNames.remove("usage_database")
                            }
                        },
                        enabled = usageExist
                    )
                }
            }
        }
    )
}

fun checkExist(context: Context, s: String): Boolean {
    return context.getDatabasePath(s + "_database").exists()
}


@Composable
fun ChecklistItem(
    item: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
//    val isChecked = remember { mutableStateOf(checked) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Text(text = item)
    }
}

fun shareDatabases(context: Context, databaseNames: List<String>) {
    val authority = "com.jianzheng.studyzero.provider"
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


@Preview
@Composable
fun PreviewExportScreen() {
    ExportScreen(navController = NavController(LocalContext.current))
}
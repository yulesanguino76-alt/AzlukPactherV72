package com.azluk.patcher.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.azluk.patcher.ui.screens.*
import com.azluk.patcher.ui.theme.AzlukTheme

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled by screens */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permission on start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permLauncher.launch(arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ))
            }
        }

        setContent {
            AzlukTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable(
                        "detail/{pkg}",
                        arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                    ) { back ->
                        val pkg = back.arguments?.getString("pkg") ?: return@composable
                        DetailScreen(pkg = pkg, navController = navController)
                    }
                    composable(
                        "patch/{pkg}",
                        arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                    ) { back ->
                        val pkg = back.arguments?.getString("pkg") ?: return@composable
                        PatchScreen(pkg = pkg, navController = navController)
                    }
                    composable("patched") {
                        PatchedFilesScreen(navController)
                    }
                    composable("tools") {
                        ToolsScreen(navController)
                    }
                }
            }
        }
    }
}

// BroadcastReceiver for PackageInstaller results — registered in Manifest
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        val msg    = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        // Forward to any listening activity via local broadcast
        val local = Intent("com.azluk.patcher.INSTALL_RESULT_LOCAL")
        local.putExtra(PackageInstaller.EXTRA_STATUS, status)
        local.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, msg)
        local.putExtra(Intent.EXTRA_INTENT, intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        ctx.sendBroadcast(local)
    }
}

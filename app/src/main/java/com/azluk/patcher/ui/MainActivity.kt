package com.azluk.patcher.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.azluk.patcher.ui.screens.*
import com.azluk.patcher.ui.theme.AzlukTheme

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateContent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * La interfaz siempre se crea primero.
         * El acceso a las funciones de AzlukPatcher queda bloqueado
         * mientras no exista el permiso necesario.
         */
        updateContent()

        requestRequiredPermission()
    }

    override fun onResume() {
        super.onResume()

        /*
         * Cuando el usuario vuelve desde Ajustes, comprobamos
         * nuevamente el permiso.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            updateContent()
        }
    }

    private fun hasRequiredPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRequiredPermission() {
        if (hasRequiredPermission()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )

                startActivity(intent)
            } catch (_: Exception) {
                /*
                 * Algunos dispositivos/ROM no implementan correctamente
                 * ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION.
                 *
                 * En ese caso intentamos abrir la pantalla general.
                 */
                try {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    )
                } catch (_: Exception) {
                    // El usuario seguirá bloqueado hasta conceder el permiso.
                }
            }
        } else {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun updateContent() {
        val permissionGranted = hasRequiredPermission()

        setContent {
            AzlukTheme {
                if (permissionGranted) {
                    AzlukApplication()
                } else {
                    PermissionRequiredScreen(
                        onRequestPermission = {
                            requestRequiredPermission()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AzlukApplication() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }

        composable(
            "detail/{pkg}",
            arguments = listOf(
                navArgument("pkg") {
                    type = NavType.StringType
                }
            )
        ) { back ->
            val pkg = back.arguments?.getString("pkg")
                ?: return@composable

            DetailScreen(
                pkg = pkg,
                navController = navController
            )
        }

        composable(
            "patch/{pkg}",
            arguments = listOf(
                navArgument("pkg") {
                    type = NavType.StringType
                }
            )
        ) { back ->
            val pkg = back.arguments?.getString("pkg")
                ?: return@composable

            PatchScreen(
                pkg = pkg,
                navController = navController
            )
        }

        composable("patched") {
            PatchedFilesScreen(navController)
        }

        composable("tools") {
            ToolsScreen(navController)
        }
    }
}

@Composable
private fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permiso necesario",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "AzlukPatcher necesita acceso a los archivos para funcionar. " +
                    "Debes conceder el permiso antes de acceder a la aplicación.",
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        Button(
            onClick = onRequestPermission
        ) {
            Text("Conceder permiso")
        }
    }
}

class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(
        ctx: Context,
        intent: Intent
    ) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            -999
        )

        val msg = intent.getStringExtra(
            PackageInstaller.EXTRA_STATUS_MESSAGE
        )

        val local = Intent(
            "com.azluk.patcher.INSTALL_RESULT_LOCAL"
        )

        local.putExtra(
            PackageInstaller.EXTRA_STATUS,
            status
        )

        local.putExtra(
            PackageInstaller.EXTRA_STATUS_MESSAGE,
            msg
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            local.putExtra(
                Intent.EXTRA_INTENT,
                intent.getParcelableExtra(
                    Intent.EXTRA_INTENT,
                    Intent::class.java
                )
            )
        } else {
            @Suppress("DEPRECATION")
            local.putExtra(
                Intent.EXTRA_INTENT,
                intent.getParcelableExtra<Intent>(
                    Intent.EXTRA_INTENT
                )
            )
        }

        ctx.sendBroadcast(local)
    }
}

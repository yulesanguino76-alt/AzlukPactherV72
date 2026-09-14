package com.azluk.patcher.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

class AppScanner(
    private val ctx: Context
) {

    fun getAll(): List<AppInfo> {
        val pm = ctx.packageManager

        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(
                        PackageManager.GET_META_DATA.toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(
                    PackageManager.GET_META_DATA
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }

        val result = ArrayList<AppInfo>(packages.size)

        for (application in packages) {
            try {
                val packageName = application.packageName

                val packageInfo = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    pm.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(
                        packageName,
                        0
                    )
                }

                val appName = try {
                    pm.getApplicationLabel(application).toString()
                } catch (_: Exception) {
                    packageName
                }

                val icon = try {
                    pm.getApplicationIcon(packageName)
                } catch (_: Exception) {
                    null
                }

                val apkPath = application.sourceDir

                val apkSizeMb = try {
                    File(apkPath).length() /
                        (1024f * 1024f)
                } catch (_: Exception) {
                    0f
                }

                result.add(
                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        isSystemApp =
                            (application.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        apkPath = apkPath,
                        versionName = packageInfo.versionName ?: "?",
                        apkSizeMb = apkSizeMb
                    )
                )
            } catch (_: Exception) {
                // Una aplicación problemática no debe cerrar AzlukPatcher.
            }
        }

        return result.sortedBy {
            it.appName.lowercase()
        }
    }
}

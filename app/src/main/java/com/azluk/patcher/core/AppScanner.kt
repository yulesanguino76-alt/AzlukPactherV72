package com.azluk.patcher.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

class AppScanner(private val ctx: Context) {

    fun getAll(): List<AppInfo> {
        val pm = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            PackageManager.GET_META_DATA.toLong() else PackageManager.GET_META_DATA.toLong()

        val pkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags))
        else
            @Suppress("DEPRECATION") pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return pkgs.mapNotNull { ai ->
            try {
                val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    pm.getPackageInfo(ai.packageName, PackageManager.PackageInfoFlags.of(0))
                else
                    @Suppress("DEPRECATION") pm.getPackageInfo(ai.packageName, 0)

                AppInfo(
                    packageName   = ai.packageName,
                    appName       = pm.getApplicationLabel(ai).toString(),
                    icon          = try { pm.getApplicationIcon(ai.packageName) } catch (e: Exception) { null },
                    isSystemApp   = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    apkPath       = ai.sourceDir,
                    versionName   = pi.versionName ?: "?",
                    apkSizeMb     = java.io.File(ai.sourceDir).length() / (1024f * 1024f)
                )
            } catch (e: Exception) { null }
        }.sortedBy { it.appName.lowercase() }
    }
}

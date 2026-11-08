package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val iconCache = mutableMapOf<String, Bitmap?>()

    suspend fun getInstalledLaunchableApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(launchIntent, 0)
        val selfPackage = context.packageName

        val apps = resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == selfPackage) return@mapNotNull null

            val appInfo = resolveInfo.activityInfo.applicationInfo
            val label = resolveInfo.loadLabel(packageManager).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            var versionName = ""
            try {
                val pInfo = packageManager.getPackageInfo(pkg, 0)
                versionName = pInfo.versionName ?: ""
            } catch (_: Exception) {}

            AppItem(
                packageName = pkg,
                appName = label,
                isSystemApp = isSystem,
                versionName = versionName
            )
        }

        // Sort alphabetically by app name
        apps.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    fun getAppIconBitmap(packageName: String): Bitmap? {
        if (iconCache.containsKey(packageName)) {
            return iconCache[packageName]
        }
        val bitmap = try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (_: Exception) {
            null
        }
        iconCache[packageName] = bitmap
        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}

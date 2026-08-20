package com.awd.r2cloud.ui.util

import java.util.Locale

object FileUtils {
    fun formatSize(size: Long?): String {
        if (size == null || size < 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = size.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.2f %s", value, units[unitIndex])
    }
}

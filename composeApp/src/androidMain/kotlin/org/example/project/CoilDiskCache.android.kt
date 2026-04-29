package org.example.project

import coil3.ImageLoader
import coil3.disk.DiskCache
import okio.Path.Companion.toPath

actual fun ImageLoader.Builder.applyDiskCache(): ImageLoader.Builder = diskCache {
    DiskCache.Builder()
        .directory(ProjectApplication.appContext.cacheDir.resolve("image_cache").absolutePath.toPath())
        .maxSizeBytes(50L * 1024 * 1024)
        .build()
}

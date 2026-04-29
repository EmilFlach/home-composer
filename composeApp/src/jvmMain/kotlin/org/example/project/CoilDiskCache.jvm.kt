package org.example.project

import coil3.ImageLoader
import coil3.disk.DiskCache
import okio.Path.Companion.toPath
import java.io.File

actual fun ImageLoader.Builder.applyDiskCache(): ImageLoader.Builder = diskCache {
    DiskCache.Builder()
        .directory(File(System.getProperty("user.home"), ".home-composer/image_cache").absolutePath.toPath())
        .maxSizeBytes(50L * 1024 * 1024)
        .build()
}

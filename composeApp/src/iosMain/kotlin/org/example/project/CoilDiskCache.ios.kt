package org.example.project

import coil3.ImageLoader
import coil3.disk.DiskCache
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun ImageLoader.Builder.applyDiskCache(): ImageLoader.Builder {
    val urls = NSFileManager.defaultManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
    val cachePath = (urls.firstOrNull() as? NSURL)?.path ?: return this
    return diskCache {
        DiskCache.Builder()
            .directory("$cachePath/image_cache".toPath())
            .maxSizeBytes(50L * 1024 * 1024)
            .build()
    }
}

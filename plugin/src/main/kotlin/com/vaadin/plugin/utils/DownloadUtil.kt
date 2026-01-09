package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.download.DownloadableFileDescription
import com.intellij.util.download.DownloadableFileService
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.zip.ZipFile

object DownloadUtil {

    private val LOG: Logger = Logger.getInstance(DownloadUtil::class.java)

    /**
     * Downloads resource from given url in background with progress indicator
     *
     * @param project current project
     * @param url resource url
     * @param targetFile download target file
     * @param downloaderLabel label for download indicator progress widget
     * @return list of virtual files and descriptors as completable future
     */
    fun download(
        project: Project,
        url: String,
        targetFile: Path,
        downloaderLabel: String
    ): CompletableFuture<List<Pair<VirtualFile?, DownloadableFileDescription?>?>?> {
        LOG.info("Downloading $url")
        val description =
            DownloadableFileService.getInstance().createFileDescription(url, targetFile.fileName.toString())
        val downloader = DownloadableFileService.getInstance().createDownloader(listOf(description), downloaderLabel)
        return downloader.downloadWithBackgroundProgress(targetFile.parent.toString(), project)
    }

    /**
     * Finds root directory in zip archive if present
     *
     * @param zip input file
     * @return root directory name if present
     */
    @Throws(IOException::class)
    fun getZipRootFolder(zip: File): String? {
        ZipFile(zip).use { zipFile ->
            val en = zipFile.entries()
            while (en.hasMoreElements()) {
                val zipEntry = en.nextElement()
                // we do not necessarily get a separate entry for the subdirectory when the file
                // in the ZIP archive is placed in a subdirectory, so we need to check if the
                // slash
                // is found anywhere in the path
                val indexOf = zipEntry.name.indexOf('/')
                if (indexOf >= 0) {
                    return zipEntry.name.substring(0, indexOf)
                }
            }
            return null
        }
    }
}

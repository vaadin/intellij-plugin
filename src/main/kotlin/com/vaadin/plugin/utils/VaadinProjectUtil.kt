package com.vaadin.plugin.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile

class VaadinProjectUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(VaadinProjectUtil::class.java)

        private const val NOTIFICATION_GROUP = "Vaadin"

        fun downloadAndExtract(project: Project, url: String, callback: () -> Unit) {
            val filename = "project.zip"
            LOG.info("Downloading $filename")
            val basePath: String = project.basePath!!
            val downloadedFile = File(basePath, filename)
            LOG.info("File saved to $downloadedFile")
            val description = DownloadableFileService.getInstance().createFileDescription(url, filename)
            val downloader =
                DownloadableFileService.getInstance().createDownloader(listOf(description), "Vaadin Starter Project")

            downloader.downloadWithBackgroundProgress(basePath, project).thenAccept {
                LOG.info("Extracting $downloadedFile")
                ZipUtil.extract(downloadedFile.toPath(), Path.of(basePath), null)
                // move contents from single zip directory
                getZipRootFolder(downloadedFile)?.let {
                    LOG.info("Zip contains single directory $it, moving to $basePath")
                    FileUtil.copyDirContent(File(basePath, it), File(basePath))
                    FileUtil.delete(File(basePath, it))
                }
                FileUtil.delete(downloadedFile)
                LOG.info("$downloadedFile deleted")
                VirtualFileManager.getInstance().asyncRefresh(callback)
            }
        }

        @Throws(IOException::class)
        fun getZipRootFolder(zip: File): String? {
            ZipFile(zip).use { zipFile ->
                val en = zipFile.entries()
                while (en.hasMoreElements()) {
                    val zipEntry = en.nextElement()
                    // we do not necessarily get a separate entry for the subdirectory when the file
                    // in the ZIP archive is placed in a subdirectory, so we need to check if the slash
                    // is found anywhere in the path
                    val indexOf = zipEntry.name.indexOf('/')
                    if (indexOf >= 0) {
                        return zipEntry.name.substring(0, indexOf)
                    }
                }
                return null
            }
        }

        fun notify(content: String, type: NotificationType, project: Project?) {
            Notifications.Bus.notify(
                Notification(NOTIFICATION_GROUP, content, type), project
            )
        }

    }

}

package com.vaadin.plugin.utils

import com.intellij.ide.plugins.PluginManager
import com.intellij.java.library.JavaLibraryUtil.hasLibraryClass
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.starter.DownloadableModel
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile

internal const val VAADIN_SERVICE = "com.vaadin.flow.server.VaadinService"

internal const val ENDPOINTS_PLUGIN_ID = "com.intellij.microservices.ui"

class VaadinProjectUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(VaadinProjectUtil::class.java)

        val PROJECT_DOWNLOADED_PROP_KEY = Key<GraphProperty<Boolean>>("vaadin_project_downloaded")

        val PROJECT_MODEL_PROP_KEY = Key<GraphProperty<DownloadableModel?>>("vaadin_project_model")

        fun downloadAndExtract(project: Project, url: String) {
            val filename = "project.zip"
            LOG.info("Downloading $url")
            val basePath: String = project.basePath!!
            val downloadedFile = File(basePath, filename)
            LOG.info("File saved to $downloadedFile")
            val description = DownloadableFileService.getInstance().createFileDescription(url, filename)
            val downloader =
                DownloadableFileService.getInstance().createDownloader(listOf(description), "Vaadin Project")

            downloader.downloadWithBackgroundProgress(basePath, project).thenApply {
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
                VirtualFileManager.getInstance().syncRefresh()
                (project.getUserData(PROJECT_DOWNLOADED_PROP_KEY) as GraphProperty<Boolean>).set(true)
            }
        }

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
}

internal fun doNotifyAboutVaadinProject(project: Project) {
    val publisher: VaadinProjectListener = project.messageBus.syncPublisher(VaadinProjectListener.TOPIC)
    publisher.vaadinProjectDetected(project)
}

internal fun hasVaadin(project: Project): Boolean = hasLibraryClass(project, VAADIN_SERVICE)

internal fun hasVaadin(module: com.intellij.openapi.module.Module): Boolean = hasLibraryClass(module, VAADIN_SERVICE)

internal fun hasEndpoints(): Boolean =
    PluginId.findId(ENDPOINTS_PLUGIN_ID)?.let { PluginManager.isPluginInstalled(it) } ?: false

internal fun isUltimate(): Boolean = ApplicationInfo.getInstance().apiVersion.startsWith("IU-")

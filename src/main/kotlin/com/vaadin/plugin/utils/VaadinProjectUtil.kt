package com.vaadin.plugin.utils

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name

class VaadinProjectUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(VaadinProjectUtil::class.java)

        fun downloadAndExtract(project: Project, url: String) {
            val filename = Path.of(url).name
            LOG.info("Downloading $filename")
            val basePath: String = project.basePath!!
            val downloadedFile = File(basePath, filename)
            LOG.info("File saved to $downloadedFile")
            val description = DownloadableFileService.getInstance().createFileDescription(url, filename)
            val downloader =
                DownloadableFileService.getInstance().createDownloader(listOf(description), "Vaadin Starter Project")

            WriteCommandAction.runWriteCommandAction(project, "Create Vaadin Project", "Vaadin", {
                downloader.downloadWithBackgroundProgress(basePath, project).thenApply {
                    var firstEntry: String? = null
                    LOG.info("Extracting $downloadedFile")
                    ZipUtil.extract(downloadedFile.toPath(), Path.of(basePath)) { dir, file ->
                        if (firstEntry == null) {
                            firstEntry = file
                        }
                        true
                    }

                    // move contents from single zip directory
                    if (ZipUtil.isZipContainsFolder(downloadedFile)) {
                        LOG.info("Zip contains single directory $firstEntry, moving to $basePath")
                        FileUtil.moveDirWithContent(File(basePath, firstEntry!!), File(basePath))
                    }
                    FileUtil.delete(downloadedFile)
                    LOG.info("$downloadedFile deleted")
                }
            })
        }

    }

}

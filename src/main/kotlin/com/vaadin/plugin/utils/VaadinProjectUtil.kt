package com.vaadin.plugin.utils

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import com.vaadin.plugin.starter.DownloadableModel
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile
import org.jetbrains.jps.model.java.JavaResourceRootType

class VaadinProjectUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(VaadinProjectUtil::class.java)

        private const val VAADIN_LIB_PREFIX = "com.vaadin:"

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

        fun isVaadinProject(project: Project): Boolean {
            return ModuleManager.getInstance(project).modules.any { isVaadinModule(it) }
        }

        fun isVaadinModule(module: com.intellij.openapi.module.Module): Boolean {
            var hasVaadin = false
            ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library: Library ->
                if (library.name?.contains(VAADIN_LIB_PREFIX) == true) {
                    hasVaadin = true
                }
                true
            }
            return hasVaadin
        }

        fun isResource(project: Project, vfsFile: VirtualFile): Boolean {
            val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vfsFile)!!
            val list = ModuleRootManager.getInstance(module).getSourceRoots(JavaResourceRootType.RESOURCE)
            // find matching resource root for given resource file
            return list.any { vfsFile.path.startsWith(it.path) }
        }

        fun copyResource(project: Project, vfsFile: VirtualFile) {
            val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vfsFile)!!
            val list = ModuleRootManager.getInstance(module).getSourceRoots(JavaResourceRootType.RESOURCE)
            // find matching resource root for given resource file
            val resourceRoot = list.find { vfsFile.path.startsWith(it.path) }
            val resourceRelativeParentPath = vfsFile.parent.path.substringAfter(resourceRoot!!.path)
            val output = CompilerPaths.getModuleOutputPath(module, false)
            val resourceOutput = VfsUtil.createDirectories(output + resourceRelativeParentPath)
            LOG.info("Copying resource: ${vfsFile.path} to $resourceOutput")
            runWriteAction { VfsUtil.copyFile(this, vfsFile, resourceOutput) }
        }
    }
}

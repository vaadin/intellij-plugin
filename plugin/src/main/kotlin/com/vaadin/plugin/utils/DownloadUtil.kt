package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.getIfSingle
import com.intellij.util.download.DownloadableFileDescription
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import com.intellij.util.net.HttpConfigurable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.name
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object DownloadUtil {

    private val LOG: Logger = Logger.getInstance(DownloadUtil::class.java)

    /**
     * Open connection to given url using IDE proxy if configured
     *
     * @param urlStr resource URL
     * @return content as String
     */
    fun openUrlWithProxy(urlStr: String): String {
        val config = HttpConfigurable.getInstance()
        val url = URL(urlStr)

        val connection =
            if (config.USE_HTTP_PROXY && !config.isProxyException(url.toURI())) {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(config.PROXY_HOST, config.PROXY_PORT))

                if (config.PROXY_AUTHENTICATION) {
                    java.net.Authenticator.setDefault(
                        object : java.net.Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(
                                    config.proxyLogin, config.plainProxyPassword?.toCharArray())
                            }
                        })
                }

                url.openConnection(proxy)
            } else {
                url.openConnection()
            }

        return connection.getInputStream().bufferedReader().use { it.readText() }
    }

    /**
     * Downloads and extracts zip or tar.gz resource from given url.
     *
     * @param project current project
     * @param url resource url
     * @param targetFile download target file
     * @param downloaderLabel label for download indicator progress widget
     * @param moveSingleDir moves contents of single extracted directory
     */
    fun downloadAndExtract(
        project: Project,
        url: String,
        targetFile: Path,
        downloaderLabel: String,
        moveSingleDir: Boolean
    ): CompletableFuture<Void> {
        return download(project, url, targetFile, downloaderLabel).thenAccept {
            if (it == null || it.isEmpty()) {
                LOG.warn("Cannot download ${url}, please try again")
                return@thenAccept
            }
            LOG.info("Downloaded $url to $targetFile, extracting...")
            extract(targetFile, targetFile.parent, moveSingleDir)
            FileUtil.delete(targetFile)
        }
    }

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
     * Extracts zip and tar.gz files. If extracted file contains single directory, moves content to parent and removes
     * it
     *
     * @param file archive file
     * @param outputPath output directory
     * @param moveSingleDir moves contents of single extracted directory to outputPath
     * @return true if completed successfully, false otherwise
     */
    fun extract(file: Path, outputPath: Path, moveSingleDir: Boolean): Boolean {
        LOG.info("Extracting $file")

        if (file.extension == "zip") {
            ZipUtil.extract(file, outputPath, null)
            LOG.info("Zip $file extracted")
        } else if (file.name.endsWith("tar.gz")) {
            extractTarGz(file, outputPath)
            LOG.info("Tar.gz $file extracted")
        } else {
            LOG.warn("Unsupported file extension $file")
            return false
        }

        // move contents from single zip directory
        if (moveSingleDir) {
            Files.list(outputPath)
                .filter { paths -> !paths.isHidden() && paths.isDirectory() }
                .getIfSingle()
                ?.let {
                    LOG.info("Archive contains single directory $it, moving to $outputPath")
                    FileUtil.moveDirWithContent(it.toFile(), outputPath.toFile())
                }
        }

        return true
    }

    private fun extractTarGz(tarGzPath: Path, outputDirPath: Path) {
        val buffer = ByteArray(4096)
        val input = TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(tarGzPath.toFile())))

        var entry = input.nextEntry
        while (entry != null) {
            val outFile = outputDirPath.resolve(entry.name)
            if (entry.isDirectory) {
                FileUtil.createDirectory(outFile.toFile())
            } else {
                FileUtil.createDirectory(outFile.parent.toFile())
                FileOutputStream(outFile.toFile()).use { out ->
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        out.write(buffer, 0, len)
                    }
                }
            }
            entry = input.nextEntry
        }
        input.close()
    }
}

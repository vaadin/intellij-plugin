package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.download.DownloadableFileDescription
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.net.HttpConfigurable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.zip.ZipFile
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
     * Extracts tar gz archive
     *
     * @param tarGzPath archive file
     * @param outputDirPath output dir
     */
    fun extractTarGz(tarGzPath: File, outputDirPath: File) {
        val buffer = ByteArray(4096)
        val input = TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(tarGzPath)))

        var entry = input.nextEntry
        while (entry != null) {
            val outFile = outputDirPath.resolve(entry.name)
            if (entry.isDirectory) {
                FileUtil.createDirectory(outFile)
            } else {
                FileUtil.createDirectory(outFile.parentFile)
                FileOutputStream(outFile).use { out ->
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

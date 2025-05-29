package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import com.intellij.util.net.HttpConfigurable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.extension
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object DownloadUtil {

    private val LOG: Logger = Logger.getInstance(DownloadUtil::class.java)

    fun openUrlWithIntelliJProxy(urlStr: String): String {
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

    fun download(
        project: Project,
        url: String,
        downloadedFile: Path,
        downloaderLabel: String,
        extractZip: Boolean,
        callback: (Path) -> Unit
    ) {
        LOG.info("Downloading $url")
        val description =
            DownloadableFileService.getInstance().createFileDescription(url, downloadedFile.fileName.toString())
        val downloader = DownloadableFileService.getInstance().createDownloader(listOf(description), downloaderLabel)

        downloader.downloadWithBackgroundProgress(downloadedFile.parent.toString(), project).thenApply {
            LOG.info("File saved to $downloadedFile")
            if (extractZip) {
                LOG.info("Extracting $downloadedFile")
                extract(downloadedFile, downloadedFile.parent, null)
                // move contents from single zip directory
                getZipRootFolder(downloadedFile)?.let {
                    LOG.info("Zip contains single directory $it, moving to ${downloadedFile.parent}")
                    FileUtil.copyDirContent(downloadedFile.parent.resolve(it).toFile(), downloadedFile.parent.toFile())
                    FileUtil.deleteRecursively(downloadedFile.parent.resolve(it))
                }
                FileUtil.delete(downloadedFile)
                LOG.info("$downloadedFile deleted")
                callback(downloadedFile.parent)
            } else {
                callback(downloadedFile)
            }
            VirtualFileManager.getInstance().syncRefresh()
        }
    }

    private fun extract(file: Path, outputDir: Path, filter: FilenameFilter?) {
        if (file.extension == "zip") {
            ZipUtil.extract(file, outputDir, filter)
        }

        if (file.extension == "gz") {
            extractTarGz(file, outputDir)
        }
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

    @Throws(IOException::class)
    private fun getZipRootFolder(zip: Path): String? {
        ZipFile(zip.toFile()).use { zipFile ->
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

package com.vaadin.plugin.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EnvironmentUtil
import com.intellij.util.download.DownloadableFileDescription
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.notify
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class AgenticChatUtil {

    companion object {

        const val AGENTIC_CHAT_RELEASES_URL = "https://cdn.vaadin.com/copilot-chat/latest.json"

        private val LOG: Logger = Logger.getInstance(AgenticChatUtil::class.java)

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class AgenticChatRelease(
            val build: Int,
            val date: String,
            val version: String,
            val chat_app_url: URL,
            val mcp_server_url: URL
        )

        data class AgenticChatInstallResult(
            val status: AgenticChatInstallStatus,
            val pathChat: Path?,
            val pathMCP: Path?
        )

        enum class AgenticChatInstallStatus {
            INSTALLED,
            ALREADY_EXISTS,
            ERROR
        }

        private var springBootProcess: Process? = null

        fun startChatApp(project: Project, agenticChatInstallResult: AgenticChatInstallResult) {
            val shellEnv = loadShellEnv().ifEmpty { captureShellEnv() }
            val userPath = shellEnv["PATH"]
            val target = agenticChatInstallResult.pathChat?.toFile()?.absolutePath

            if (target.isNullOrEmpty() || !File(target).exists()) {
                notify("Chat App JAR file not found. Please download it first.", NotificationType.ERROR, project)
                return
            }

            if (springBootProcess?.isAlive == true || isChatAppRunning()) {
                notify("Chat App is already running.", NotificationType.WARNING, project)
                return
            }

            try {
                val processBuilder = ProcessBuilder("java", "-jar", target)
                val fullEnv = ProcessBuilder().environment()
                fullEnv.replace("PATH", fullEnv.get("PATH") + ":" + userPath)
                processBuilder.environment().putAll(fullEnv)
                processBuilder.redirectErrorStream(true)
                springBootProcess = processBuilder.start()

                Thread {
                        springBootProcess?.inputStream?.bufferedReader()?.use { reader ->
                            reader.lines().forEach { LOG.info("[ChatApp] $it") }
                        }
                    }
                    .start()
            } catch (e: IOException) {
                LOG.error("Failed to start Chat App: ${e.message}", e)
                notify("Failed to start Chat App: ${e.message}", NotificationType.ERROR, project)
            }
            notify("Chat App started successfully.", NotificationType.INFORMATION, project)
        }

        private fun isChatAppRunning(): Boolean {
            return try {
                val url = java.net.URL("http://localhost:9090")
                with(url.openConnection() as java.net.HttpURLConnection) {
                    connectTimeout = 1000
                    readTimeout = 1000
                    requestMethod = "GET"
                    connect()
                    responseCode in 200..399
                }
            } catch (e: IOException) {
                false
            }
        }

        fun stopChatApp(project: Project) {
            try {
                if (springBootProcess?.isAlive == true) {
                    springBootProcess?.destroy()
                    springBootProcess?.waitFor()
                    notify("Chat App stopped successfully.", NotificationType.INFORMATION, project)
                } else {
                    notify("Chat App is not running.", NotificationType.WARNING, project)
                }
            } catch (e: Exception) {
                LOG.error("Failed to stop Chat App: ${e.message}", e)
                notify("Failed to stop Chat App: ${e.message}", NotificationType.ERROR, project)
            }
        }

        /**
         * Checks if the latest version of the Copilot Chat JAR file exists in the Vaadin home directory. So far version
         * is not checked, only the file existence.
         */
        fun existsLatestCopilotChatJar(): Boolean {
            val target = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("ai/copilot-chat.jar")
            return target.exists() && target.isFile
        }

        /**
         * Checks if the latest version of the Copilot Local MCP Server JAR file exists in the Vaadin home directory. So
         * far version is not checked, only the file existence.
         */
        fun existsLatestCopilotLocalMcpServerJar(): Boolean {
            val target = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("ai/copilot-local-mcp-server.jar")
            return target.exists() && target.isFile
        }

        private fun getFilename(url: URL): String = url.file.replace(".*/".toRegex(), "")

        fun downloadLatestAgenticChatRelease(project: Project): CompletableFuture<AgenticChatInstallResult> {
            val latest = findLatestArtifactsRelease()
            if (latest == null || (latest.chat_app_url.file.isEmpty() || latest.mcp_server_url.file.isEmpty())) {
                LOG.error("Unable to fetch latest Agentic Chat release info")
                return CompletableFuture.completedFuture(
                    AgenticChatInstallResult(status = AgenticChatInstallStatus.ERROR, pathChat = null, pathMCP = null))
            }
            val latestChatArtifact = getFilename(latest.chat_app_url)
            val latestMcpArtifact = getFilename(latest.mcp_server_url)
            val chatTarget = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("ai/$latestChatArtifact")
            val mcpTarget = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("ai/$latestMcpArtifact")
            if (chatTarget.exists() && mcpTarget.exists()) {
                LOG.info(
                    "Latest Agentic Chat release already exists at ${chatTarget.absolutePath} and ${mcpTarget.absolutePath}")
                return CompletableFuture.completedFuture(
                    AgenticChatInstallResult(
                        status = AgenticChatInstallStatus.ALREADY_EXISTS,
                        pathChat = chatTarget.toPath(),
                        pathMCP = mcpTarget.toPath()))
            }
            if (!chatTarget.exists()) {
                LOG.info("Downloading Agentic Chat artifact into ${chatTarget.absolutePath}")
                return downloadLatestCopilotChatJar(project, latest.chat_app_url, chatTarget)
                    .thenRun {
                        if (!mcpTarget.exists()) {
                            LOG.info("Downloading Copilot MCP Server artifact into ${mcpTarget.absolutePath}")
                            downloadLatestCopilotLocalMcpServerJar(project, latest.mcp_server_url, mcpTarget)
                                .thenRun { LOG.info("Agentic Chat and MCP Server artifacts downloaded successfully.") }
                                .thenApply {
                                    AgenticChatInstallResult(
                                        status = AgenticChatInstallStatus.INSTALLED,
                                        pathChat = chatTarget.toPath(),
                                        pathMCP = mcpTarget.toPath())
                                }
                        } else {
                            LOG.info("Agentic Chat artifact downloaded successfully.")
                        }
                    }
                    .thenApply {
                        AgenticChatInstallResult(
                            status = AgenticChatInstallStatus.INSTALLED,
                            pathChat = chatTarget.toPath(),
                            pathMCP = mcpTarget.toPath())
                    }
            }
            if (!mcpTarget.exists()) {
                LOG.info("Downloading Copilot MCP Server artifact into ${mcpTarget.absolutePath}")
                downloadLatestCopilotLocalMcpServerJar(project, latest.mcp_server_url, mcpTarget)
                    .thenRun { LOG.info("MCP Server artifacts downloaded successfully.") }
                    .thenApply {
                        AgenticChatInstallResult(
                            status = AgenticChatInstallStatus.INSTALLED,
                            pathChat = chatTarget.toPath(),
                            pathMCP = mcpTarget.toPath())
                    }
            }
            return CompletableFuture.completedFuture(
                AgenticChatInstallResult(status = AgenticChatInstallStatus.ERROR, pathChat = null, pathMCP = null))
        }

        /**
         * Downloads the latest version of the Copilot Chat JAR file from the CDN and saves it to the Vaadin home
         * directory under "ai/copilot-chat.jar".
         *
         * TODO actually check the version of the downloaded file
         *
         * @param project The current project context.
         * @return A CompletableFuture that resolves to a list of pairs containing VirtualFile and
         *   DownloadableFileDescription.
         */
        fun downloadLatestCopilotChatJar(
            project: Project,
            url: URL,
            targetFile: File
        ): CompletableFuture<List<com.intellij.openapi.util.Pair<VirtualFile?, DownloadableFileDescription?>?>?> {

            val targetFolder = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("ai/")
            if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                throw IOException("Unable to create ${targetFolder.absolutePath}")
            }

            LOG.info("Downloading Copilot Chat into ${targetFile.absolutePath}")
            return DownloadUtil.download(project, url.toExternalForm(), targetFile.toPath(), "Copilot Chat")
        }

        /**
         * Downloads the latest version of the Copilot Local MCP Server JAR file from the CDN and saves it to the Vaadin
         * home directory under "ai/copilot-local-mcp-server.jar".
         *
         * TODO actually check the version of the downloaded file
         *
         * @param project The current project context.
         * @return A CompletableFuture that resolves to a list of pairs containing VirtualFile and
         *   DownloadableFileDescription.
         */
        fun downloadLatestCopilotLocalMcpServerJar(
            project: Project,
            url: URL,
            targetFile: File
        ): CompletableFuture<List<Pair<VirtualFile?, DownloadableFileDescription?>?>?> {

            val targetFolder = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("ai/")
            if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                throw IOException("Unable to create ${targetFolder.absolutePath}")
            }

            LOG.info("Downloading Copilot Chat into ${targetFile.absolutePath}")
            return DownloadUtil.download(project, url.toExternalForm(), targetFile.toPath(), "Copilot Chat")
        }

        fun loadShellEnv(): Map<String, String> {
            return try {
                EnvironmentUtil.getEnvironmentMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }

        fun captureShellEnv(): Map<String, String> {
            val shell = System.getenv("SHELL") ?: return emptyMap()
            val pb = ProcessBuilder(shell, "-ilc", "env")
            val env = mutableMapOf<String, String>()
            pb.start().inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    line
                        .indexOf('=')
                        .takeIf { it > 0 }
                        ?.let { idx ->
                            val key = line.substring(0, idx)
                            val value = line.substring(idx + 1)
                            env[key] = value
                        }
                }
            }
            return env
        }

        fun findLatestArtifactsRelease(): AgenticChatRelease? {
            try {
                val text = DownloadUtil.openUrlWithProxy(AGENTIC_CHAT_RELEASES_URL)
                return jacksonObjectMapper().readValue(text, AgenticChatRelease::class.java)
            } catch (e: Exception) {
                throw IOException("Unable to fetch JetBrains Runtime releases info", e)
            }
        }
    }
}

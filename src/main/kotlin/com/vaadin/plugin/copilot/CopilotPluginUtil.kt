package com.vaadin.plugin.copilot

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbModeTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.*
import com.vaadin.plugin.copilot.handler.*
import com.vaadin.plugin.utils.VaadinIcons
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.BufferedWriter
import java.io.IOException
import java.io.StringWriter
import java.util.*
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

class CopilotPluginUtil {

    @JvmRecord
    data class ModuleInfo(
        val name: String,
        val contentRoots: Array<String>,
        val javaSourcePaths: Array<String>,
        val javaTestSourcePaths: Array<String>,
        val resourcePaths: Array<String>,
        val testResourcePaths: Array<String>,
        val outputPath: String?
    )

    @JvmRecord data class ProjectInfo(val basePath: String?, val modules: List<ModuleInfo>)

    companion object {

        private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

        private const val DOTFILE = ".copilot-plugin"

        private const val IDEA_DIR = ".idea"

        private const val NORMALIZED_LINE_SEPARATOR = "\n"

        private enum class HANDLERS(val command: String) {
            WRITE("write"),
            WRITE_BASE64("writeBase64"),
            UNDO("undo"),
            REDO("redo"),
            REFRESH("refresh"),
            SHOW_IN_IDE("showInIde"),
            GET_MODULE_PATHS("getModulePaths"),
        }

        private val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId("com.vaadin.intellij-plugin"))?.version

        const val NOTIFICATION_GROUP = "Vaadin Copilot"

        fun getPluginVersion(): String? {
            return pluginVersion
        }

        fun notify(content: String, type: NotificationType, project: Project?) {
            Notifications.Bus.notify(
                Notification(NOTIFICATION_GROUP, content, type).setIcon(VaadinIcons.VAADIN),
                project,
            )
        }

        fun createCommandHandler(command: String, project: Project, data: Map<String, Any>): Handler {
            when (command) {
                HANDLERS.WRITE.command -> return WriteFileHandler(project, data)
                HANDLERS.WRITE_BASE64.command -> return WriteBase64FileHandler(project, data)
                HANDLERS.UNDO.command -> return UndoHandler(project, data)
                HANDLERS.REDO.command -> return RedoHandler(project, data)
                HANDLERS.SHOW_IN_IDE.command -> return ShowInIdeHandler(project, data)
                HANDLERS.REFRESH.command -> return RefreshHandler(project)
                HANDLERS.GET_MODULE_PATHS.command -> return GetModulePathsHandler(project)
                else -> {
                    LOG.warn("Command $command not supported by plugin")
                    return object : Handler {
                        override fun run(): HandlerResponse {
                            return HandlerResponse(HttpResponseStatus.BAD_REQUEST)
                        }
                    }
                }
            }
        }

        fun saveDotFile(project: Project) {
            val task =
                object : DumbModeTask() {
                    override fun performInDumbMode(progressIndicator: ProgressIndicator) {
                        saveDotFileInternal(project)
                    }
                }
            DumbService.getInstance(project).queueTask(task)
        }

        private fun saveDotFileInternal(project: Project) {
            val dotFileDirectory = getDotFileDirectory(project)
            if (dotFileDirectory != null) {
                val props = Properties()
                props.setProperty("endpoint", RestUtil.getEndpoint())
                props.setProperty("ide", "intellij")
                props.setProperty("version", pluginVersion)
                props.setProperty("supportedActions", HANDLERS.entries.joinToString(",") { a -> a.command })

                val stringWriter = StringWriter()
                val bufferedWriter =
                    object : BufferedWriter(stringWriter) {
                        override fun newLine() {
                            write(NORMALIZED_LINE_SEPARATOR)
                        }
                    }
                props.store(bufferedWriter, "Vaadin Copilot Integration Runtime Properties")
                runInEdt {
                    WriteAction.run<Throwable> {
                        val dotFile = dotFileDirectory.findFile(DOTFILE)
                        dotFile?.let {
                            try {
                                it.delete(this)
                            } catch (e: IOException) {
                                LOG.error("Failed to delete $DOTFILE: ${e.message}")
                            }
                        }
                        val newFile =
                            try {
                                dotFileDirectory.createChildData(this, DOTFILE)
                            } catch (e: IOException) {
                                LOG.error("Failed to create $DOTFILE: ${e.message}")
                                return@run
                            }

                        try {
                            VfsUtil.saveText(newFile, stringWriter.toString())
                        } catch (e: IOException) {
                            LOG.error("Failed to write to $DOTFILE: ${e.message}")
                        }
                        LOG.info("$newFile created")
                    }
                }
            }
        }

        fun removeDotFile(project: Project) {
            runInEdt {
                WriteAction.run<Throwable> {
                    val dotFile = getDotFileDirectory(project)?.findFile(DOTFILE)
                    dotFile?.let {
                        try {
                            it.delete(this)
                            LOG.info("$it removed")
                        } catch (e: IOException) {
                            LOG.error("Failed to delete $DOTFILE: ${e.message}")
                        }
                        return@run
                    }
                    LOG.warn("Cannot remove $dotFile - file does not exist")
                }
            }
        }

        private fun getDotFileDirectory(project: Project): VirtualFile? {
            val projectDir = project.guessProjectDir()
            if (projectDir == null) {
                LOG.error("Cannot guess project directory")
                return null
            }
            LOG.info("Project directory: $projectDir")
            return projectDir.findOrCreateDirectory(IDEA_DIR)
        }

        fun getDotFile(project: Project): VirtualFile? {
            return getDotFileDirectory(project)?.findFile(DOTFILE)
        }

        /**
         * Returns a list of all modules in the project. Each module contains information about the module name, content
         * roots, source paths, test source paths, resource paths, test resource paths, and output path.
         */
        fun getModulesInfo(project: Project): ArrayList<ModuleInfo> {
            val modules = ArrayList<ModuleInfo>()
            ModuleManager.getInstance(project).modules.forEach { module ->
                val moduleRootManager = ModuleRootManager.getInstance(module)
                val contentRoots = moduleRootManager.contentRoots.map { it.path }

                val compilerModuleExtension = CompilerModuleExtension.getInstance(module)
                val outputPath = compilerModuleExtension?.compilerOutputPath

                // Note that the JavaSourceRootType.SOURCE also includes Kotlin source folders
                // Only include folder if is is not in the output path
                val javaSourcePaths = moduleRootManager.getSourceRoots(JavaSourceRootType.SOURCE).map({ it.path })
                val javaTestSourcePaths =
                    moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).map({ it.path })

                val resourcePaths = moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE).map { it.path }
                val testResourcePaths =
                    moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE).map { it.path }

                modules.add(
                    ModuleInfo(
                        module.name,
                        contentRoots.toTypedArray(),
                        javaSourcePaths.toTypedArray(),
                        javaTestSourcePaths.toTypedArray(),
                        resourcePaths.toTypedArray(),
                        testResourcePaths.toTypedArray(),
                        outputPath?.path))
            }
            return modules
        }

        /**
         * Returns a map of all base directories related to the project. This includes any external module and the main
         * project base folders. For the main project, the base directory is the project root and the module name is
         * "base-module".
         */
        fun getBaseDirectoriesForProject(project: Project): Map<String, List<String>> {

            val moduleBaseDirectories = mutableMapOf<String, List<String>>()
            getModulesInfo(project).forEach { module ->
                moduleBaseDirectories[module.name] = module.contentRoots.toList()
            }
            moduleBaseDirectories["base-module"] = listOf(project.basePath) as List<String>
            return moduleBaseDirectories
        }
    }
}

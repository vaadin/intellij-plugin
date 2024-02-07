package com.vaadin.plugin.copilot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.handlers.WriteFileAction
import java.io.File
import java.io.FileWriter
import java.util.Properties

class CopilotServerProjectAction : AnAction() {

    private val LOG: Logger = Logger.getInstance(CopilotServerProjectAction::class.java)

    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project
        if (project == null) {
            notify("Copilot Plugin can be started only on existing project", NotificationType.WARNING)
            return
        }

        val psiDirectoryNode = p0.dataContext.getData(CommonDataKeys.NAVIGATABLE_ARRAY)!!.first() as PsiDirectoryNode

        val server = project.service<CopilotServerService>()
        if (server.isRunning()) {
            server.stop()
            removeDotFile(psiDirectoryNode)
            notify("Copilot Plugin Stopped", NotificationType.INFORMATION)
            return
        }

        server.init()
        savePortInDotFile(psiDirectoryNode, server.getPort()!!)
        ApplicationManager.getApplication().executeOnPooledThread {
            notify("Copilot Plugin Started", NotificationType.INFORMATION)
            server.start { data ->
                handleClientData(project, data)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY)?.first() is PsiDirectoryNode

        if (e.project?.service<CopilotServerService>()?.isRunning() == true) {
            e.presentation.text = "Stop Copilot Plugin"
        } else {
            e.presentation.text = "Start Copilot Plugin Here"
        }
    }

    private fun handleClientData(project: Project, data: ByteArray) {
        val command: CommandRequest = jacksonObjectMapper().readValue(data)
        LOG.info("Running action " + command.command)
        runInEdt {
            createCommandHandler(command.command, project, command.data)?.run()
        }
    }

    private fun createCommandHandler(
        command: String,
        project: Project,
        data: Map<String, Any>
    ): Runnable? {
        when (command) {
            "write" -> return WriteFileAction(project, data)
        }
        return null
    }

    private fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(Notification("Copilot", message, type))
    }

    private fun savePortInDotFile(psiDirectoryNode: PsiDirectoryNode, port: Int) {
        if (psiDirectoryNode.virtualFile != null) {
            val ioFile = File(psiDirectoryNode.virtualFile!!.canonicalPath + File.separator + ".copilot-plugin")
            val props = Properties()
            props.setProperty("port", port.toString())
            props.setProperty("ide", "intellij")
            props.setProperty("version", PluginManagerCore.getPlugin(PluginId.getId("com.vaadin.intellij-plugin"))?.version)
            props.store(FileWriter(ioFile), "Copilot Plugin Runtime Properties")
        }
    }

    private fun removeDotFile(psiDirectoryNode: PsiDirectoryNode) {
        if (psiDirectoryNode.virtualFile != null) {
            val ioFile = File(psiDirectoryNode.virtualFile!!.canonicalPath + File.separator + ".copilot-plugin")
            ioFile.delete()
        }
    }

}
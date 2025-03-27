package com.vaadin.plugin.copilot.handler

import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

/**
 * Handler for restarting the application. If the main class is provided, the handler will restart the application with
 * the provided main class.
 */
class RestartApplicationHandler(project: Project, data: Map<String, Any>?) : AbstractHandler(project) {

    private val expectedMainClass: String? = data?.get("mainClass") as String?

    override fun run(): HandlerResponse {
        val contentManager = RunContentManager.getInstance(project)

        val descriptor = findMatchingDescriptor(contentManager) ?: return RESPONSE_BAD_REQUEST

        runInEdt { ExecutionUtil.restart(descriptor) }

        return RESPONSE_OK
    }

    private fun findMatchingDescriptor(runContentManager: RunContentManager): RunContentDescriptor? {
        // If the main class is not provided, use default descriptor
        if (expectedMainClass == null) {
            return runContentManager.selectedContent
        }

        // Find descriptor with matching main class
        for (descriptor in runContentManager.allDescriptors) {
            val processHandler = descriptor.processHandler

            // Support only BaseOSProcessHandler
            if (processHandler !is BaseOSProcessHandler) {
                continue
            }

            val mainClassName = processHandler.commandLine.split(" ").last()
            if (mainClassName != expectedMainClass) {
                continue
            }

            return descriptor
        }

        return null
    }
}

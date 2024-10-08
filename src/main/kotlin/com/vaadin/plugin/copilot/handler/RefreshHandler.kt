package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager

class RefreshHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        VirtualFileManager.getInstance().asyncRefresh {}
        return RESPONSE_OK
    }
}

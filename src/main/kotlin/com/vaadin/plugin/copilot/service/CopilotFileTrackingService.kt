package com.vaadin.plugin.copilot.service

import com.intellij.openapi.vfs.VirtualFile

interface CopilotFileTrackingService {

    fun getLastModified(): VirtualFile?

    fun setLastModified(document: VirtualFile)

}
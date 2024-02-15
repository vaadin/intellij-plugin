package com.vaadin.plugin.copilot.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.vfs.VirtualFile

class CopilotFileTrackingServiceImpl : CopilotFileTrackingService, PersistentStateComponent<Map<String, Any>> {

    private var state: HashMap<String, Any> = HashMap()

    override fun getLastModified(): VirtualFile? {
        return state["lastModified"] as VirtualFile
    }

    override fun setLastModified(document: VirtualFile) {
        state["lastModified"] = document
    }

    override fun getState(): Map<String, Any>? {
        return HashMap(state)
    }

    override fun loadState(p0: Map<String, Any>) {
        state = HashMap(p0)
    }
}
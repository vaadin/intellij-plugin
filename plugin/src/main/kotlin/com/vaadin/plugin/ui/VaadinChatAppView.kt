package com.vaadin.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContentUiType
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.jcef.JBCefBrowser
import com.vaadin.plugin.utils.IdeUtil
import java.util.UUID
import org.apache.http.client.utils.URIBuilder

@Service(Service.Level.PROJECT)
@State(name = "VaadinChatAppView", storages = [Storage("vaadin.xml")])
class VaadinChatAppView : SimplePersistentStateComponent<VaadinChatAppView.State>(State()), Disposable {

    val title = "Vaadin AI Chat"
    val urlRoot = "http://localhost:9090"
    var counter = 1

    val SESSION_ID: Key<String> = Key.create("SESSION_ID")

    override fun dispose() {}

    class State : BaseState() {
        var sessions: List<String> = ArrayList()
        var titles: HashMap<String, String> = HashMap()
        var selectedSession: String? = null
        var aiProvider: String = "ANY" // TODO: should be mapped to settings
    }

    fun initToolWindow(toolWindow: ToolWindow) {
        toolWindow.title = title
        toolWindow.stripeTitle = title
        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.TABBED)

        state.sessions.forEach { createSession(toolWindow.contentManager, it, state.titles[it]!!) }

        toolWindow.contentManager.contents
            .find { c -> c.getUserData(SESSION_ID) == state.selectedSession }
            ?.let { toolWindow.contentManager.setSelectedContent(it) }

        toolWindow.addContentManagerListener(
            object : ContentManagerListener {
                override fun contentAdded(event: ContentManagerEvent) {
                    val sessionId = event.content.getUserData(SESSION_ID) as String
                    state.sessions += sessionId
                    state.titles[sessionId] = event.content.toolwindowTitle
                    state.intIncrementModificationCount()
                }

                override fun contentRemoved(event: ContentManagerEvent) {
                    val sessionId = event.content.getUserData(SESSION_ID) as String
                    state.sessions -= sessionId
                    state.titles.remove(sessionId)
                    if (state.sessions.isEmpty()) {
                        state.selectedSession = null
                    }
                    state.intIncrementModificationCount()
                }

                override fun selectionChanged(event: ContentManagerEvent) {
                    state.selectedSession = event.content.getUserData(SESSION_ID) as String
                    state.intIncrementModificationCount()
                }
            })

        toolWindow.setTitleActions(listOf(NewChatSessionAction()))
    }

    fun createNewSession(contentManager: ContentManager) {
        val sessionId = UUID.randomUUID().toString()
        createSession(contentManager, sessionId, "Chat " + counter++)
    }

    private fun createSession(contentManager: ContentManager, sessionId: String, title: String) {
        val uri = URIBuilder(urlRoot).addParameter("sessionId", sessionId).addParameter("theme", getTheme()).build()
        val browser = JBCefBrowser(uri.toString())
        val content = contentManager.factory.createContent(browser.component, title, false)
        content.putUserData(SESSION_ID, sessionId)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)
    }

    private fun getTheme(): String {
        return if (IdeUtil.isDarkTheme()) "dark" else "light"
    }

    private class NewChatSessionAction : AnAction() {
        init {
            templatePresentation.icon = AllIcons.General.Add
            templatePresentation.text = "New Session"
            templatePresentation.description = "Start new Vaadin AI Chat session"
        }

        override fun actionPerformed(event: AnActionEvent) {
            val toolWindow = event.getData(PlatformDataKeys.TOOL_WINDOW)
            if (toolWindow != null) {
                event.project?.service<VaadinChatAppView>()?.createNewSession(toolWindow.contentManager)
            }
        }
    }
}

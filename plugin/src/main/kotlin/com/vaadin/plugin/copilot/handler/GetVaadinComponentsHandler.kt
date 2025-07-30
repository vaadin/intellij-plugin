package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.vaadin.plugin.endpoints.findComponents
import com.vaadin.plugin.endpoints.signatureToString
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinComponentsHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val components = findComponents(project, GlobalSearchScope.allScope(project))

            val mapComponents =
                components.map { component ->
                    mapOf(
                        "class" to component.className,
                        "origin" to component.origin,
                        "source" to component.source,
                        "path" to component.path,
                        "methods" to component.visibleMethods.joinToString(",") { signatureToString(it) })
                }

            LOG.info("Components detected: $components")

            HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("components" to mapComponents))
        }
    }
}

package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.vaadin.plugin.endpoints.findFlowRoutes
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinRoutesHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        val flowViews = findFlowRoutes(project, GlobalSearchScope.allScope(project))

        val mapFlowRoute =
            flowViews.map { route -> mapOf("route" to route.urlMapping, "classname" to route.locationString) }

        LOG.info("Flow Routes detected: $flowViews")

        return HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("routes" to mapFlowRoute))
    }
}

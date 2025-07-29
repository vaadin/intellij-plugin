package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.vaadin.plugin.endpoints.findEntities
import com.vaadin.plugin.endpoints.signatureToString
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinPersistenceHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val entities = findEntities(project, GlobalSearchScope.allScope(project))

            val mapEntities =
                entities.map { entity ->
                    mapOf(
                        "classname" to entity.className,
                        "methods" to entity.visibleMethods.joinToString(",") { signatureToString(it) },
                        "path" to entity.path)
                }

            LOG.info("Entities detected: $entities")

            HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("entities" to mapEntities))
        }
    }
}

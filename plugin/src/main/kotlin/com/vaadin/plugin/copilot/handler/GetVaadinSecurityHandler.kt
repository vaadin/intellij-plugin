package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.vaadin.plugin.endpoints.findSecurityConfig
import com.vaadin.plugin.endpoints.findUserDetails
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinSecurityHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val security = findSecurityConfig(project, GlobalSearchScope.allScope(project))
            val userDetails = findUserDetails(project, GlobalSearchScope.allScope(project))

            val mapSecurity =
                security.map { component ->
                    mapOf(
                        "class" to component.className,
                        "origin" to component.origin,
                        "source" to component.source,
                        "path" to component.path,
                        "loginView" to component.loginView)
                }
            val mapUserDetails =
                userDetails.map { component ->
                    mapOf(
                        "class" to component.className,
                        "origin" to component.origin,
                        "source" to component.source,
                        "path" to component.path)
                }

            LOG.info("Security detected: $security")

            HandlerResponse(
                status = HttpResponseStatus.OK,
                data = mapOf("security" to mapSecurity, "userDetails" to mapUserDetails))
        }
    }
}

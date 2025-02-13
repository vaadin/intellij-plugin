package com.vaadin.plugin.endpoints

import com.intellij.microservices.endpoints.EndpointType
import com.intellij.microservices.endpoints.EndpointsFilter
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.HTTP_SERVER_TYPE
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.uast.UastModificationTracker
import com.vaadin.plugin.utils.hasVaadin

abstract class VaadinEndpointsProvider : EndpointsProvider<VaadinRoute, VaadinRoute> {

    override val endpointType: EndpointType = HTTP_SERVER_TYPE

    override fun getStatus(project: Project): EndpointsProvider.Status {
        if (hasVaadin(project)) return EndpointsProvider.Status.HAS_ENDPOINTS
        return EndpointsProvider.Status.UNAVAILABLE
    }

    override fun getModificationTracker(project: Project): ModificationTracker {
        return UastModificationTracker.getInstance(project)
    }

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<VaadinRoute> {
        if (filter !is ModuleEndpointsFilter) return emptyList()
        if (!hasVaadin(filter.module)) return emptyList()

        return findFlowRoutes(project, filter.transitiveSearchScope)
    }

    override fun getEndpoints(group: VaadinRoute): Iterable<VaadinRoute> {
        return listOf(group)
    }

    override fun isValidEndpoint(group: VaadinRoute, endpoint: VaadinRoute): Boolean {
        return group.isValid()
    }

    override fun getDocumentationElement(group: VaadinRoute, endpoint: VaadinRoute): PsiElement? {
        return endpoint.anchor.retrieve()
    }
}

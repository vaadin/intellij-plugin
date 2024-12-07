package com.vaadin.plugin.endpoints

import com.intellij.microservices.endpoints.EndpointType
import com.intellij.microservices.endpoints.EndpointsFilter
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.EndpointsProvider.Status
import com.intellij.microservices.endpoints.FrameworkPresentation
import com.intellij.microservices.endpoints.HTTP_SERVER_TYPE
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.microservices.endpoints.presentation.HttpUrlPresentation
import com.intellij.microservices.url.UrlPath
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.uast.UastModificationTracker
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.hasVaadin

internal class VaadinEndpointsProvider : EndpointsProvider<VaadinRoute, VaadinRoute> {
    override val endpointType: EndpointType = HTTP_SERVER_TYPE

    override val presentation: FrameworkPresentation =
        FrameworkPresentation("Vaadin", "Vaadin Flow", VaadinIcons.VAADIN_BLUE)

    override fun getStatus(project: Project): Status {
        if (hasVaadin(project)) return Status.HAS_ENDPOINTS

        return Status.UNAVAILABLE
    }

    override fun getModificationTracker(project: Project): ModificationTracker {
        return UastModificationTracker.getInstance(project)
    }

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<VaadinRoute> {
        if (filter !is ModuleEndpointsFilter) return emptyList()
        if (!hasVaadin(filter.module)) return emptyList()

        return findVaadinRoutes(project, filter.transitiveSearchScope)
    }

    override fun getEndpoints(group: VaadinRoute): Iterable<VaadinRoute> {
        return listOf(group)
    }

    override fun isValidEndpoint(group: VaadinRoute, endpoint: VaadinRoute): Boolean {
        return group.isValid()
    }

    override fun getEndpointPresentation(group: VaadinRoute, endpoint: VaadinRoute): ItemPresentation {
        return HttpUrlPresentation(normalizeUrl(group.urlMapping), group.locationString, VaadinIcons.VAADIN_BLUE)
    }

    private fun normalizeUrl(urlMapping: String): String {
        val urlString = run {
            if (urlMapping.isBlank()) return@run "/"
            if (!urlMapping.startsWith("/")) return@run "/$urlMapping"
            return@run urlMapping
        }

        return parseVaadinUrlMapping(urlString).getPresentation(VaadinUrlRenderer)
    }

    override fun getDocumentationElement(group: VaadinRoute, endpoint: VaadinRoute): PsiElement? {
        return endpoint.anchor.retrieve()
    }
}

private object VaadinUrlRenderer : UrlPath.PathSegmentRenderer {
    override fun visitVariable(variable: UrlPath.PathSegment.Variable): String {
        return "{${variable.variableName}}"
    }
}

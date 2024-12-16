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

internal class VaadinEndpointsProvider : EndpointsProvider<VaadinEndpointGroup, VaadinRoute> {
    override val endpointType: EndpointType = HTTP_SERVER_TYPE

    override val presentation: FrameworkPresentation =
        FrameworkPresentation("Vaadin", "Vaadin", VaadinIcons.VAADIN_BLUE)

    override fun getStatus(project: Project): Status {
        if (hasVaadin(project)) return Status.HAS_ENDPOINTS

        return Status.UNAVAILABLE
    }

    override fun getModificationTracker(project: Project): ModificationTracker {
        return UastModificationTracker.getInstance(project)
    }

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<VaadinEndpointGroup> {
        if (filter !is ModuleEndpointsFilter) return emptyList()
        if (!hasVaadin(filter.module)) return emptyList()

        return listOf(
            VaadinEndpointGroup(project, filter, ENDPOINT_GROUP.FLOW),
            VaadinEndpointGroup(project, filter, ENDPOINT_GROUP.HILLA))
    }

    override fun getEndpoints(group: VaadinEndpointGroup): Iterable<VaadinRoute> {
        if (group.endPointGroup === ENDPOINT_GROUP.FLOW)
            return findFlowRoutes(group.project, group.filter.transitiveSearchScope)
        if (group.endPointGroup === ENDPOINT_GROUP.HILLA)
            return findHillaEndpoints(group.project, group.filter.transitiveSearchScope)
        return emptyList()
    }

    override fun isValidEndpoint(group: VaadinEndpointGroup, endpoint: VaadinRoute): Boolean {
        return endpoint.isValid()
    }

    override fun getEndpointPresentation(group: VaadinEndpointGroup, endpoint: VaadinRoute): ItemPresentation {
        val item =
            HttpUrlPresentation(
                normalizeUrl(group, endpoint.urlMapping), endpoint.locationString, VaadinIcons.VAADIN_BLUE)
        return item
    }

    private fun normalizeUrl(group: VaadinEndpointGroup, urlMapping: String): String {
        val urlString = run {
            if (urlMapping.isBlank()) return@run "/"
            if (group.endPointGroup == ENDPOINT_GROUP.FLOW && !urlMapping.startsWith("/")) return@run "/$urlMapping"
            if (group.endPointGroup == ENDPOINT_GROUP.HILLA) return@run urlMapping
            return@run urlMapping
        }

        return parseVaadinUrlMapping(urlString).getPresentation(VaadinUrlRenderer)
    }

    override fun getDocumentationElement(group: VaadinEndpointGroup, endpoint: VaadinRoute): PsiElement? {
        return endpoint.anchor.retrieve()
    }
}

private object VaadinUrlRenderer : UrlPath.PathSegmentRenderer {
    override fun visitVariable(variable: UrlPath.PathSegment.Variable): String {
        return "{${variable.variableName}}"
    }
}

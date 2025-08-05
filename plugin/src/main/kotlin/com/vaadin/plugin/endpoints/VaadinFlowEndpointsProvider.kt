package com.vaadin.plugin.endpoints

import com.intellij.microservices.endpoints.EndpointsFilter
import com.intellij.microservices.endpoints.FrameworkPresentation
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.microservices.endpoints.presentation.HttpUrlPresentation
import com.intellij.microservices.url.UrlPath
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.hasVaadin

internal class VaadinFlowEndpointsProvider : VaadinEndpointsProvider() {

    override val presentation: FrameworkPresentation =
        FrameworkPresentation("Vaadin-Flow", "Vaadin Flow", VaadinIcons.VAADIN_BLUE)

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<VaadinRoute> {
        if (filter !is ModuleEndpointsFilter) return emptyList()
        if (!hasVaadin(filter.module)) return emptyList()

        return findFlowRoutes(project, filter.transitiveSearchScope, null)
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

    private object VaadinUrlRenderer : UrlPath.PathSegmentRenderer {
        override fun visitVariable(variable: UrlPath.PathSegment.Variable): String {
            return "{${variable.variableName}}"
        }
    }
}

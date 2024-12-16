package com.vaadin.plugin.endpoints

import com.intellij.microservices.endpoints.EndpointsFilter
import com.intellij.microservices.endpoints.FrameworkPresentation
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.microservices.endpoints.presentation.HttpUrlPresentation
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.hasVaadin

internal class VaadinHillaEndpointsProvider : VaadinEndpointsProvider() {

    override val presentation: FrameworkPresentation =
        FrameworkPresentation("Vaadin", "Vaadin Hilla", VaadinIcons.VAADIN_BLUE)

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<VaadinRoute> {
        if (filter !is ModuleEndpointsFilter) return emptyList()
        if (!hasVaadin(filter.module)) return emptyList()

        return findHillaEndpoints(project, filter.transitiveSearchScope)
    }

    override fun getEndpointPresentation(group: VaadinRoute, endpoint: VaadinRoute): ItemPresentation {
        return HttpUrlPresentation(group.urlMapping, group.locationString, VaadinIcons.VAADIN_BLUE)
    }
}

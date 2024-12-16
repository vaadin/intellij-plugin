package com.vaadin.plugin.endpoints

import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.openapi.project.Project

internal enum class ENDPOINT_GROUP {
    HILLA,
    FLOW
}

internal class VaadinEndpointGroup(
    val project: Project,
    val filter: ModuleEndpointsFilter,
    val endPointGroup: ENDPOINT_GROUP
)

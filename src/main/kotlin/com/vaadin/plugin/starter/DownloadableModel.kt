package com.vaadin.plugin.starter

import com.intellij.openapi.project.Project

interface DownloadableModel {

    fun getDownloadLink(project: Project): String

    fun getProjectType(): String
}

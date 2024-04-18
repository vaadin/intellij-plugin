package com.vaadin.plugin.starter

import com.intellij.openapi.project.Project

interface HasDownloadLink {

    fun getDownloadLink(project: Project): String

}
package com.vaadin.plugin.module

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.runBlockingModal
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.startup.ProjectActivity

// TODO: Remove this after fixing Maven/Gradle module loading
class VaadinPostStartupProjectClose : ProjectActivity {

    override suspend fun execute(project: Project) {
        ProjectUtil.getOpenProjects().filter { it != project && it.name == project.name }.forEach {
            it.closeProjectAsync(false)
        }
    }

    // copy com.intellij.testFramework.closeProjectAsync
    private suspend fun Project.closeProjectAsync(save: Boolean = false) {
        if (ApplicationManager.getApplication().isDispatchThread) {
            runBlockingModal(this, "") {
                ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(this@closeProjectAsync, save = save)
            }
        } else {
            ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(this, save = save)
        }
    }

}
package com.vaadin.plugin.module

import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.vaadin.plugin.utils.VaadinProjectUtil
import java.io.File

class PostModuleCreatedActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.getUserData(VaadinProjectUtil.PROJECT_DOWNLOADED_PROP_KEY)?.afterChange {
            VaadinProjectUtil.notify("Vaadin project created", NotificationType.INFORMATION, project)
            VfsUtil.findFileByIoFile(File(project.basePath, "README.md"), true)?.let {
                val descriptor = OpenFileDescriptor(project, it)
                descriptor.setUsePreviewTab(true)
                FileEditorManager.getInstance(project).openEditor(descriptor, true)
            }
        }
    }

}
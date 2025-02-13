package com.vaadin.plugin.actions

import com.intellij.codeInsight.actions.onSave.ActionOnSaveInfoBase
import com.intellij.ide.actionsOnSave.ActionOnSaveComment
import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project

class VaadinCompileOnSaveActionInfo(context: ActionOnSaveContext) :
    ActionOnSaveInfoBase(context, NAME, PROPERTY, DEFAULT) {

    companion object {
        const val ID = "VaadinCompileOnSaveAction"
        const val NAME = "Compile Java files"
        const val PROPERTY = "vaadin.compileOnSave"
        const val DEFAULT = true
        const val DESCRIPTION = "Compiles *.java while debugging"

        fun isEnabledForProject(project: Project): Boolean {
            return PropertiesComponent.getInstance(project).getBoolean(PROPERTY, DEFAULT)
        }

        fun getAction(): VaadinCompileOnSaveAction {
            return ActionManager.getInstance().getAction(ID) as VaadinCompileOnSaveAction
        }
    }

    override fun getComment(): ActionOnSaveComment? {
        return ActionOnSaveComment.info(DESCRIPTION)
    }
}

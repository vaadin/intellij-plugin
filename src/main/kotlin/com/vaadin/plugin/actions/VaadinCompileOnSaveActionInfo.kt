package com.vaadin.plugin.actions

import com.intellij.codeInsight.actions.onSave.ActionOnSaveInfoBase
import com.intellij.ide.actionsOnSave.ActionOnSaveComment
import com.intellij.ide.actionsOnSave.ActionOnSaveContext

class VaadinCompileOnSaveActionInfo(context: ActionOnSaveContext) :
    ActionOnSaveInfoBase(context, NAME, PROPERTY, DEFAULT) {

    companion object {
        const val NAME = "Compile Java files"
        const val PROPERTY = "vaadin.compileOnSave"
        const val DEFAULT = true
        const val DESCRIPTION = "Compiles *.java while debugging"
    }

    override fun getComment(): ActionOnSaveComment? {
        return ActionOnSaveComment.info(DESCRIPTION)
    }
}

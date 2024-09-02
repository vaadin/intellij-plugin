package com.vaadin.plugin.actions

import com.intellij.codeInsight.actions.onSave.ActionOnSaveInfoBase
import com.intellij.ide.actionsOnSave.ActionOnSaveComment
import com.intellij.ide.actionsOnSave.ActionOnSaveContext

class VaadinCompileOnSaveActionInfo(context: ActionOnSaveContext) :
    ActionOnSaveInfoBase(context, NAME, PROPERTY, DEFAULT) {

    companion object {
        const val NAME = "Vaadin - compile on save"
        const val PROPERTY = "vaadin.compileOnSave"
        const val DEFAULT = true
        const val DESCRIPTION = "Compiles Java files on save to improve Flow applications development experience"
    }

    override fun getComment(): ActionOnSaveComment? {
        return ActionOnSaveComment.info(DESCRIPTION)
    }
}

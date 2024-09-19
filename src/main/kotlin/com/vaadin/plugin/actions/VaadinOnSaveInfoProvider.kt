package com.vaadin.plugin.actions

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider

class VaadinOnSaveInfoProvider : ActionOnSaveInfoProvider() {

    override fun getActionOnSaveInfos(p0: ActionOnSaveContext): MutableCollection<out ActionOnSaveInfo> {
        val info: ArrayList<ActionOnSaveInfo> = ArrayList()
        info.add(VaadinCompileOnSaveActionInfo(p0))
        return info
    }
}

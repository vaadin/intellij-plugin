package com.vaadin.plugin.module

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class VaadinModuleType(id: @NonNls String) : ModuleType<VaadinModuleBuilder>(id) {

    override fun createModuleBuilder(): VaadinModuleBuilder {
        return VaadinModuleBuilder()
    }

    override fun getName(): String {
        return "Vaadin"
    }

    override fun getDescription(): String {
        return "Create Vaadin application"
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return IconLoader.getIcon("/META-INF/pluginIcon.svg", javaClass.classLoader)
    }

}

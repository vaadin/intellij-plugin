package com.vaadin.plugin.utils

import com.intellij.openapi.util.IconLoader

class VaadinIcons {

    companion object {

        private const val RESOURCE_PATH = "/vaadin/icons"

        val VAADIN_BLUE = IconLoader.getIcon("$RESOURCE_PATH/module.svg", javaClass::class.java)

        val VAADIN = IconLoader.getIcon("$RESOURCE_PATH/vaadin.svg", javaClass::class.java)

        val DEBUG_HOTSWAP = IconLoader.getIcon("$RESOURCE_PATH/swap.svg", javaClass::class.java)

    }

}

package com.vaadin.plugin.utils

import com.intellij.openapi.util.IconLoader

class VaadinIcons {

    companion object {

        private const val RESOURCE_PATH = "/vaadin/icons"

        val VAADIN_BLUE = IconLoader.getIcon("$RESOURCE_PATH/module.svg", VaadinIcons::class.java.classLoader)

        val VAADIN = IconLoader.getIcon("$RESOURCE_PATH/vaadin.svg", VaadinIcons::class.java.classLoader)

        val DEBUG_HOTSWAP = IconLoader.getIcon("$RESOURCE_PATH/swap.svg", VaadinIcons::class.java.classLoader)

    }

}

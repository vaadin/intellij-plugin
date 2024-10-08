package com.vaadin.plugin.hotswapagent

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

class BadJBRFoundDialog() : DialogWrapper(true) {

    private val message: String

    override fun createActions(): Array<Action> {
        return arrayOf(myOKAction)
    }

    init {
        message = buildString {
            append(
                "HotswapAgent requires running with a JetBrains Runtime JDK which implements the low level hotswap functionality.\n\n")
            append("Your IntelliJ IDEA installation includes a bundled JetBrains Runtime JDK which is known not work for this purpose.\n\n")
            append("You can resolve this by one of the following:\n")
                append("- Downgrade IntelliJ IDEA to version 2024.2.2 or earlier which bundle a working version\n");
                append("- Once released, upgrade IntelliJ IDEA to a version newer than 2024.2.3\n");
            append(
                "- Download a newer JetBrains runtime from https://github.com/JetBrains/JetBrainsRuntime/releases and set your run configuration to use it.")
            append("\n")
        }
        title = "Unable to Find a Suitable JDK"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel().apply { add(JTextArea(message)) }
    }
}

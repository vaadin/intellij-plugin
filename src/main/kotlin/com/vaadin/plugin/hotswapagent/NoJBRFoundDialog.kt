package com.vaadin.plugin.hotswapagent

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

class NoJBRFoundDialog(bundledJetbrainsJdkMajor: Int?, projectJdkMajor: Int?) :
    DialogWrapper(true) {

    private val message: String

    override fun createActions(): Array<Action> {
        return arrayOf(myOKAction)
    }

    init {
        val upgradeIntellijHelps =
            (bundledJetbrainsJdkMajor != null &&
                    projectJdkMajor != null &&
                    bundledJetbrainsJdkMajor < 21 &&
                    projectJdkMajor <= 21)

        message = buildString {
            append(
                "HotswapAgent requires running with a JetBrains Runtime JDK which implements the low level hotswap functionality.\n\n"
            )
            append(
                "Your IntelliJ IDEA installation includes a bundled JetBrains Runtime JDK for Java "
            )
            append(bundledJetbrainsJdkMajor ?: "?")
            append(".\n\n")
            append("Your project is configured to use Java ")
            append(projectJdkMajor ?: "?")
            append(".\n\n")
            append("You can resolve this by one of the following:\n")
            if (upgradeIntellijHelps) {
                append(
                    "- Upgrade IntelliJ IDEA to version 2024.2 or later which bundles JBR 21."
                )
                append("\n")
            }
            append(
                "- Download a newer JetBrains runtime from https://github.com/JetBrains/JetBrainsRuntime/releases and set your run configuration to use it."
            )
            append("\n")
            append("- Change the project Java version to ")
            append(bundledJetbrainsJdkMajor ?: " the Jetbrains JDK version")
            append("\n")
        }
        title = "Unable to Find a Suitable JDK"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel().apply { add(JTextArea(message)) }
    }
}

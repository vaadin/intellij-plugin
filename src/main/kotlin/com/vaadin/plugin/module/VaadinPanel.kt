package com.vaadin.plugin.module

import com.intellij.ide.wizard.withVisualPadding
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.HasDownloadLink

class VaadinPanel {

    private var dialogPanel: DialogPanel? = null

    private var quickStarterGroup: CollapsibleRow? = null
    private var demoStarterGroup: CollapsibleRow? = null

    private val quickStarterPanel = QuickStarterPanel()
    private val skeletonStarterPanel = SkeletonStarterPanel()

    init {
        dialogPanel = panel {
            row {
                text("Quick start local application development with a starter project based on your favorite tech stack.")
            }
            row {
                text(
                    "<a href=\"https://vaadin.com/flow\">Flow framework</a> is the most productive" +
                            " choice, allowing 100% of the user interface to be coded in server-side Java."
                )
            }
            row {
                text(
                    "<a href=\"https://hilla.dev/\">Hilla framework</a>, on the other hand, enables" +
                            " implementation of your user interface with React while automatically connecting it to your" +
                            " Java backend."
                )
            }

            quickStarterGroup = collapsibleGroup("Quick Starter") {
                row {}.cell(quickStarterPanel.root)
            }

            demoStarterGroup = collapsibleGroup("Demo Starters") {
                row {}.cell(skeletonStarterPanel.root!!)
            }
            separator()
            row {
                text("You can generate more complex Vaadin start applications on <a href=\"https://start.vaadin.com/\">start.vaadin.com</a>")
            }
            row {
                text("Read more at <a href=\"https://vaadin.com/docs\">vaadin.com/docs</a>")
            }
        }.withVisualPadding(true)

        quickStarterGroup!!.expanded = true
        quickStarterGroup!!.addExpandedListener { if (it) demoStarterGroup!!.expanded = false }
        demoStarterGroup!!.addExpandedListener { if (it) quickStarterGroup!!.expanded = false }
    }

    fun getComponent(): DialogPanel {
        return dialogPanel!!
    }

    fun getModel(): HasDownloadLink {
        return if (quickStarterGroup!!.expanded) quickStarterPanel.model else skeletonStarterPanel.model
    }

}

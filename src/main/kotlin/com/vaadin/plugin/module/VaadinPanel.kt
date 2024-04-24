package com.vaadin.plugin.module

import com.intellij.ide.wizard.withVisualPadding
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.DownloadableModel

class VaadinPanel {

    private var dialogPanel: DialogPanel? = null

    private var quickStarterGroup: CollapsibleRow? = null
    private var skeletonStarterGroup: CollapsibleRow? = null

    private val quickStarterPanel = QuickStarterPanel()
    private val skeletonStarterPanel = SkeletonStarterPanel()

    init {
        dialogPanel = panel {
            quickStarterGroup = collapsibleGroup("Project Settings") {
                row {}.cell(quickStarterPanel.root)
            }

            skeletonStarterGroup = collapsibleGroup("Hello World Projects") {
                row {}.cell(skeletonStarterPanel.root)
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
            row {
                text("For more configuration options, visit <a href=\"https://start.vaadin.com\">start.vaadin.com</a>")
            }
        }.withVisualPadding(true)

        quickStarterGroup!!.expanded = true
        quickStarterGroup!!.addExpandedListener { if (it) skeletonStarterGroup!!.expanded = false }
        skeletonStarterGroup!!.addExpandedListener { if (it) quickStarterGroup!!.expanded = false }
    }

    fun getComponent(): DialogPanel {
        return dialogPanel!!
    }

    fun getModel(): DownloadableModel {
        return if (quickStarterGroup!!.expanded) quickStarterPanel.model else skeletonStarterPanel.model
    }

}
